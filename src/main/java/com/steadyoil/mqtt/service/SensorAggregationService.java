package com.steadyoil.mqtt.service;

import com.steadyoil.mqtt.domain.SensorDataAgg;
import com.steadyoil.mqtt.dto.SensorDataSummary;
import com.steadyoil.mqtt.repository.SensorDataAggRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import jakarta.validation.Valid;
import lombok.extern.apachecommons.CommonsLog;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@CommonsLog
@Service
public class SensorAggregationService {
    @Autowired
    private SequenceGeneratorService sequenceGeneratorService;
    @Autowired
    private SensorDataAggRepository sensorDataAggRepository;

    @Autowired
    private MongoClient mongoClient;

    @Value("${com.steadyoil.mongodb.host}")
    private String host;

    @Value("${com.steadyoil.mongodb.port}")
    private String port;
    @Value("${com.steadyoil.mongodb.database}")
    private String database;

    @Value("${com.steadyoil.mongodb.username}")
    private String username;

    @Value("${com.steadyoil.mongodb.password}")
    private String password;

    @Value("${com.steadyoil.mongodb.collections.data_in}")
    private String collection;

    @Value("${com.steadyoil.mongodb.collections.data_agg_1_min}")
    private String collectionAgg;

    @Value("${com.steadyoil.client.saveAgg.offsetMin}")
    private int offsetMinutes;

    public SensorDataSummary aggregatedData1(String apiKey, int lastHours) {
        MongoDatabase mongoDatabase = mongoClient.getDatabase(this.database);
        MongoCollection<Document> mongoCollection = mongoDatabase.getCollection(collection);

        // @formatter:off
        List<Document> listAgg = new ArrayList<>();
        listAgg.add(new Document("$match", new Document("timestamp", new Document("$gte", new Date((new Date()).getTime() - TimeUnit.HOURS.toMillis(lastHours))))
                .append("key", new Document("$eq", apiKey))));

        listAgg.add(new Document("$sort", new Document("timestamp", -1L)));

        listAgg.add(new Document("$group", new Document("_id", new Document("$dateToString", new Document("format", "%Y-%m-%dT%H")
                .append("date", "$timestamp")))
                .append("key", new Document("$addToSet", "$key"))
                .append("timestamps", new Document("$push", new Document("$dateToString", new Document("format", "%Y-%m-%dT%H:%M:%S")
                        .append("date", "$timestamp"))))
                .append("air_temperature", new Document("$push", "$air_temperature"))
                .append("water_temperature", new Document("$push", "$water_temperature"))
                .append("compressor_temperature", new Document("$push", "$compressor_temperature"))
                .append("liquid_pressure", new Document("$push", "$liquid_pressure"))
                .append("suction_pressure", new Document("$push", "$suction_pressure"))
                .append("water_flow", new Document("$push", "$water_flow"))));

        listAgg.add(new Document("$project", new Document("_id", 0L)
                .append("key", new Document("$first", "$key"))
                .append("timestamps", 1L)
                .append("air_temperature", new Document("$mergeObjects", new Document("min", new Document("$min", "$air_temperature"))
                                .append("max", new Document("$max", "$air_temperature"))
                                .append("avg", new Document("$avg", "$air_temperature"))
                                .append("values", "$air_temperature")))
                .append("water_temperature", new Document("$mergeObjects", new Document("min", new Document("$min", "$water_temperature"))
                        .append("max", new Document("$max", "$water_temperature"))
                        .append("avg", new Document("$avg", "$water_temperature"))
                        .append("values", "$water_temperature")))
                .append("compressor_temperature", new Document("$mergeObjects", new Document("min", new Document("$min", "$compressor_temperature"))
                                .append("max", new Document("$max", "$compressor_temperature"))
                .append("avg", new Document("$avg", "$compressor_temperature"))
                        .append("values", "$compressor_temperature")))
                .append("liquid_pressure", new Document("$mergeObjects", new Document("min", new Document("$min", "$liquid_pressure"))
                        .append("max", new Document("$max", "$liquid_pressure"))
                        .append("avg", new Document("$avg", "$liquid_pressure"))
                        .append("values", "$liquid_pressure")))
                .append("suction_pressure", new Document("$mergeObjects", new Document("min", new Document("$min", "$suction_pressure"))
                        .append("max", new Document("$max", "$suction_pressure"))
                        .append("avg", new Document("$avg", "$suction_pressure"))
                        .append("values", "$suction_pressure")))
                .append("water_flow", new Document("$mergeObjects", new Document("min", new Document("$min", "$water_flow"))
                        .append("max", new Document("$max", "$water_flow"))
                        .append("avg", new Document("$avg", "$water_flow"))
                        .append("values", "$water_flow")))));
        // @formatter:on

        return getSensorDataSummary(mongoCollection, listAgg);
    }

    private SensorDataSummary getSensorDataSummary(MongoCollection<Document> mongoCollection, List<Document> listAgg) {
        AggregateIterable<Document> documentAggregateIterable = mongoCollection.aggregate(listAgg);

        Document document = documentAggregateIterable.first();
        ObjectMapper mapper = new ObjectMapper();
        SensorDataSummary sensorDataSummary = new SensorDataSummary();
        try {
            sensorDataSummary = mapper.readValue(document.toJson(), SensorDataSummary.class);
        } catch (Exception e) {
            log.error(String.format("Aggregation json error: %s", e.getMessage()));
        }

        return sensorDataSummary;
    }

    /**
     * Aggregation of data for graph points in single object.
     *
     * @param apiKey
     * @param lastHours
     * @param sampling
     * @return SensorDataSummary
     */
    public SensorDataSummary aggregatedData2(String apiKey, int lastHours, int sampling, @Valid List<Long> dateRange) {
        final long dateTimeNow = (new Date()).getTime();
        final boolean realTime = false;

        MongoDatabase mongoDatabase = mongoClient.getDatabase(this.database);
        MongoCollection<Document> mongoCollection;

        if (realTime) {
            mongoCollection = mongoDatabase.getCollection(collection);
        } else {
            mongoCollection = mongoDatabase.getCollection(collectionAgg);
        }
        Date dateStart;
        Date dateEnd;

        if (dateRange.size() == 2) {
            dateStart = new Date(dateRange.get(0));
            dateEnd = new Date(dateRange.get(1));
        } else {
            dateStart = new Date(dateTimeNow - TimeUnit.HOURS.toMillis(lastHours) - TimeUnit.MINUTES.toMillis(offsetMinutes + 1));
            dateEnd = new Date(dateTimeNow - TimeUnit.MINUTES.toMillis(offsetMinutes));
        }


        // @formatter:off
        List<Document> listAgg = new ArrayList<>();
        listAgg.add(new Document("$match", new Document("timestamp", new Document("$gte", dateStart).append("$lt", dateEnd))
                .append("key", new Document("$eq", apiKey))));

        listAgg.add(new Document("$sort", new Document("timestamp", -1L)));

        listAgg.add(new Document("$group", new Document("_id", new Document("$subtract", Arrays.asList(new Document("$toLong", "$timestamp"), new Document("$mod", Arrays.asList(new Document("$toLong", "$timestamp"), 1000L * 60L * sampling)))))
                        .append("key", new Document("$addToSet", "$key"))
                        .append("avg_air_temperature", new Document("$avg", "$air_temperature"))
                        .append("avg_water_temperature", new Document("$avg", "$water_temperature"))
                        .append("avg_compressor_temperature", new Document("$avg", "$compressor_temperature"))
                        .append("avg_liquid_pressure", new Document("$avg", "$liquid_pressure"))
                        .append("avg_suction_pressure", new Document("$avg", "$suction_pressure"))
                        .append("avg_water_flow", new Document("$avg", "$water_flow"))));

        listAgg.add(new Document("$sort", new Document("_id", 1L)));

        listAgg.add(new Document("$group", new Document("_id", 0L)
                        .append("key", new Document("$first", "$key"))
                        .append("timestamps", new Document("$push", new Document("$dateToString", new Document("format", "%Y-%m-%dT%H:%M:%S")
                                .append("date", new Document("$add", Arrays.asList(new Date(0L), "$_id"))))))
                        .append("avg_air_temperature", new Document("$push", new Document("$round", Arrays.asList("$avg_air_temperature", 2L))))
                        .append("avg_water_temperature", new Document("$push", new Document("$round", Arrays.asList("$avg_water_temperature", 2L))))
                        .append("avg_compressor_temperature", new Document("$push", new Document("$round", Arrays.asList("$avg_compressor_temperature", 2L))))
                        .append("avg_liquid_pressure", new Document("$push", new Document("$round", Arrays.asList("$avg_liquid_pressure", 2L))))
                        .append("avg_suction_pressure", new Document("$push", new Document("$round", Arrays.asList("$avg_suction_pressure", 2L))))
                        .append("avg_water_flow", new Document("$push", new Document("$round", Arrays.asList("$avg_water_flow", 2L))))));


        listAgg.add(new Document("$project", new Document("_id", 0L)
                        .append("key", new Document("$first", "$key"))
                        .append("timestamps", 1L)
                        .append("air_temperature", new Document("$mergeObjects", new Document("min", new Document("$min", "$avg_air_temperature"))
                                                .append("max", new Document("$max", "$avg_air_temperature"))
                                                .append("avg", new Document("$round", Arrays.asList(new Document("$avg", "$avg_air_temperature"), 2L)))
                                                .append("values", "$avg_air_temperature")))
                        .append("water_temperature", new Document("$mergeObjects", new Document("min", new Document("$min", "$avg_water_temperature"))
                                                .append("max", new Document("$max", "$avg_water_temperature"))
                                                .append("avg", new Document("$round", Arrays.asList(new Document("$avg", "$avg_water_temperature"), 2L)))
                                                .append("values", "$avg_water_temperature")))
                        .append("compressor_temperature", new Document("$mergeObjects", new Document("min", new Document("$min", "$avg_compressor_temperature"))
                                                .append("max", new Document("$max", "$avg_compressor_temperature"))
                                                .append("avg", new Document("$round", Arrays.asList(new Document("$avg", "$avg_compressor_temperature"), 2L)))
                                                .append("values", "$avg_compressor_temperature")))
                        .append("liquid_pressure", new Document("$mergeObjects", new Document("min", new Document("$min", "$avg_liquid_pressure"))
                                                .append("max", new Document("$max", "$avg_liquid_pressure"))
                                                .append("avg", new Document("$round", Arrays.asList(new Document("$avg", "$avg_liquid_pressure"), 2L)))
                                                .append("values", "$avg_liquid_pressure")))
                        .append("suction_pressure", new Document("$mergeObjects", new Document("min", new Document("$min", "$avg_suction_pressure"))
                                                .append("max", new Document("$max", "$avg_suction_pressure"))
                                                .append("avg", new Document("$round", Arrays.asList(new Document("$avg", "$avg_suction_pressure"), 2L)))
                                                .append("values", "$avg_suction_pressure")))
                        .append("water_flow", new Document("$mergeObjects", new Document("min", new Document("$min", "$avg_water_flow"))
                                                .append("max", new Document("$max", "$avg_water_flow"))
                                                .append("avg", new Document("$round", Arrays.asList(new Document("$avg", "$avg_water_flow"), 2L)))
                                                .append("values", "$avg_water_flow")))));
        // @formatter:on

        return getSensorDataSummary(mongoCollection, listAgg);
    }

    /**
     * Aggregation of all data by given sampling
     *
     * @param lastMinutes
     * @param samplingMinutes
     * @return List<SensorData>
     */
    public List<SensorDataAgg> aggregatedData3(String apiKey, int lastMinutes, int samplingMinutes) {
        long last_id;

        SensorDataAgg sensorDataAggFirst = sensorDataAggRepository.findFirstByKeyOrderByLastIdDesc(apiKey);
        if (sensorDataAggFirst == null) {
            last_id = 0;
        } else {
            last_id = sensorDataAggFirst.getLastId();
        }

        MongoDatabase mongoDatabase = mongoClient.getDatabase(this.database);
        MongoCollection<Document> mongoCollection = mongoDatabase.getCollection(collection);

        // @formatter:off
        List<Document> listAgg = new ArrayList<>();

        if(last_id == 0){
            listAgg.add(new Document("$match", new Document("timestamp", new Document("$gte",
                                    new Date((new Date()).getTime() - TimeUnit.MINUTES.toMillis(lastMinutes)) ))
                                    .append("key", new Document("$eq", apiKey))
            ));
        } else {
            listAgg.add(new Document("$match", new Document("timestamp", new Document("$lte", new Date()))
                    .append("key", new Document("$eq", apiKey))
                    .append("_id", new Document("$gt", last_id))));
        }

        listAgg.add(new Document("$sort", new Document("timestamp", -1L)));

        listAgg.add(new Document("$group", new Document("_id", new Document("$subtract", Arrays.asList(new Document("$toLong", "$timestamp"),
                new Document("$mod", Arrays.asList(new Document("$toLong", "$timestamp"), 1000L * 60L * samplingMinutes)))))
                .append("last_id", new Document("$first", "$_id"))
                .append("key", new Document("$first", "$key"))
                .append("avg_air_temperature", new Document("$avg", "$air_temperature"))
                .append("avg_water_temperature", new Document("$avg", "$water_temperature"))
                .append("avg_compressor_temperature", new Document("$avg", "$compressor_temperature"))
                .append("avg_liquid_pressure", new Document("$avg", "$liquid_pressure"))
                .append("avg_suction_pressure", new Document("$avg", "$suction_pressure"))
                .append("avg_water_flow", new Document("$avg", "$water_flow"))));

        listAgg.add(new Document("$addFields", new Document("air_temperature", new Document("$round", Arrays.asList("$avg_air_temperature", 2L)))
                .append("water_temperature", new Document("$round", Arrays.asList("$avg_water_temperature", 2L)))
                .append("compressor_temperature", new Document("$round", Arrays.asList("$avg_compressor_temperature", 2L)))
                .append("liquid_pressure", new Document("$round", Arrays.asList("$avg_liquid_pressure", 2L)))
                .append("suction_pressure", new Document("$round", Arrays.asList("$avg_suction_pressure", 2L)))
                .append("water_flow", new Document("$round", Arrays.asList("$avg_water_flow", 2L)))));

        listAgg.add(new Document("$sort", new Document("_id", 1L)));

        listAgg.add(new Document("$project", new Document("timestamp", new Document("$dateToString", new Document("format", "%Y-%m-%dT%H:%M:%S")
                        .append("date", new Document("$add", Arrays.asList(new Date(0L), "$_id")))))
                .append("_id", 0L)
                .append("last_id", 1L)
                .append("key", 1L)
                .append("air_temperature", 1L)
                .append("water_temperature", 1L)
                .append("compressor_temperature", 1L)
                .append("liquid_pressure", 1L)
                .append("suction_pressure", 1L)
                .append("water_flow", 1L)));

        // @formatter:on
        AggregateIterable<Document> documentAggregateIterable = mongoCollection.aggregate(listAgg);

        ObjectMapper mapper = new ObjectMapper();
        List<SensorDataAgg> sensorDataList = new ArrayList<>();

        documentAggregateIterable.forEach(document -> {
            try {
                sensorDataList.add(mapper.readValue(document.toJson(), SensorDataAgg.class));
            } catch (JsonProcessingException e) {
                log.error(String.format("Aggregation 3 json error: %s", e.getMessage()));
            }
        });

        return sensorDataList;
    }

    @Transactional
    public void aggregatedData4() {
        log.info("data-copy start");
        long last_id;

        SensorDataAgg sensorDataAggFirst = sensorDataAggRepository.findFirstByOrderByIdDesc();
        if (sensorDataAggFirst == null) {
            last_id = 0;
        } else {
            last_id = sensorDataAggFirst.getId();
        }

        MongoDatabase mongoDatabase = mongoClient.getDatabase(this.database);
        MongoCollection<Document> mongoCollection = mongoDatabase.getCollection(collection);

        // @formatter:off
        List<Document> listAgg = new ArrayList<>();
        listAgg.add(new Document("$match", new Document("_id", new Document("$gt", last_id))));
        listAgg.add(new Document("$out", "data-copy"));
        // @formatter:on

        mongoCollection.aggregate(listAgg).toCollection();
        log.info("data-copy end");
    }

    @Async
    @Transactional
    public void saveAggDataList(List<SensorDataAgg> sensorDataAggList) {
        for (int i = 0; i < sensorDataAggList.size(); i++) {
            sensorDataAggList.get(i).setId(sequenceGeneratorService.generateSequence(SensorDataAgg.SEQUENCE_NAME));
        }
        sensorDataAggRepository.saveAll(sensorDataAggList);
    }

}
