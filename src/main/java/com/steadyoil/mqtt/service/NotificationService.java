package com.steadyoil.mqtt.service;

import com.steadyoil.mqtt.domain.Notification;
import com.steadyoil.mqtt.domain.Sensor;
import com.steadyoil.mqtt.domain.SensorData;
import com.steadyoil.mqtt.dto.NotificationSummary;
import com.steadyoil.mqtt.repository.NotificationRepository;
import com.steadyoil.mqtt.util.AESUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import jakarta.validation.Valid;
import lombok.extern.apachecommons.CommonsLog;
import org.bson.BsonNull;
import org.bson.Document;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@CommonsLog
@Service
public class NotificationService {
    @Autowired
    private SequenceGeneratorService sequenceGeneratorService;
    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private SensorService sensorService;

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

    @Value("${com.steadyoil.mongodb.collections.notifications}")
    private String collection;

    /**
     * Method to check threshold boundaries for every sensor property and create notification if exceeds.
     *
     * @param sensorData
     */
    @Transactional
    @Async
    public void createNotificationAsync(SensorData sensorData) {
        String key = sensorData.getKey();
        log.debug("createNotificationAsync key:" + key);
        String sid = AESUtil.getSensorId(key);
        log.debug("createNotificationAsync sid:" + sid);
        long l = Long.parseLong(sid);
        log.debug("createNotificationAsync long:" + l);
        Sensor sensor = sensorService.getSensorById(l);
        log.debug("createNotificationAsync sensor:" + sensor.toString());
        if (sensor == null) {return;}
        Sensor.Thresholds thresholds = sensor.getThresholds();

        for (Sensor.SensorProperty p : Sensor.SensorProperty.values()) {
            String thresholdProperty = p.toString();
            String thresholdPropertyStr =
                    Arrays.stream(thresholdProperty.split("_")).map((s) -> s.substring(0, 1).toUpperCase() + s.substring(1)).collect(Collectors.joining(""));

            Sensor.Threshold threshold;
            double dataValue;
            try {
                Method sensorThresholdGetter = thresholds.getClass().getMethod("get" + thresholdPropertyStr);
                threshold = (Sensor.Threshold) sensorThresholdGetter.invoke(thresholds);

                Method sensorDataValueGetter = sensorData.getClass().getMethod("get" + thresholdPropertyStr);
                dataValue = Double.valueOf(sensorDataValueGetter.invoke(sensorData).toString());

            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                log.error("Notification methods error:" + e.getMessage());
                throw new RuntimeException(e);
            }

            if (threshold.isEnabled()) {
                String thresholdBoundary = "none";
                if (dataValue <= threshold.getLow()) {
                    thresholdBoundary = "low";
                } else if (dataValue >= threshold.getHigh()) {
                    thresholdBoundary = "high";
                }

                if (!thresholdBoundary.equals("none")) {
                    double thresholdValue;

                    try {
                        Method thresholdValueGetter =
                                threshold.getClass().getMethod("get" + thresholdBoundary.substring(0, 1).toUpperCase() + thresholdBoundary.substring(1));
                        thresholdValue = Double.valueOf(thresholdValueGetter.invoke(threshold).toString());
                    } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                        log.error("Notification methods error:" + e.getMessage());
                        throw new RuntimeException(e);
                    }

                    Notification notification = new Notification();
                    notification.setId(sequenceGeneratorService.generateSequence(Notification.SEQUENCE_NAME));
                    notification.setSensorKey(sensorData.getKey());
                    notification.setTimestamp(new Date());
                    notification.setThresholdBoundary(thresholdBoundary);
                    notification.setThresholdProperty(thresholdProperty);
                    notification.setDataId(sensorData.getId());
                    notification.setDataTimestamp(sensorData.getTimestamp());
                    notification.setDataValue(dataValue);
                    notification.setThresholdValue(thresholdValue);
                    //        notification.isRead = false;

                    String label = thresholdBoundary.substring(0, 1).toUpperCase() + thresholdBoundary.substring(1) + " " + thresholdPropertyStr;
                    String description =
                            "Data Value " + notification.getDataValue() + " is " + notification.getThresholdBoundary().toUpperCase() + "-er" + " " + "than the Threshold Value " + notification.getThresholdValue() + " for " + thresholdPropertyStr;

                    notification.setLabel(label);
                    notification.setDescription(description);

                    log.info(notification.toString());
                    notificationRepository.save(notification);
                }
            }
        }
    }


    public Page<Notification> getNotificationsByKeysAndLastHours(List<String> keys, int lastHours, Pageable pageable) {
        Date timestamp = new Date((new Date()).getTime() - TimeUnit.HOURS.toMillis(lastHours));
        return notificationRepository.findBySensorKeyInAndTimestampAfter(keys, timestamp, pageable);
    }

    @Transactional
    public Notification updateNotificationIsRead(long id, boolean isRead) {
        Notification notification = getNotificationById(id);
        notification.setRead(isRead);
        notificationRepository.save(notification);
        return notification;
    }

    private Notification getNotificationById(long notificationId) {
        Optional<Notification> notificationDb = this.notificationRepository.findById(notificationId);

        if (notificationDb.isPresent()) {
            return notificationDb.get();
        } else {
            throw new Error("Record not found with id : " + notificationId);
        }
    }

    public Page<NotificationSummary> aggregatedData(List<String> listApiKey, int lastHours, @Valid Pageable page, boolean inMinutes,
                                                    List<Long> dateRange) {
        return aggregatedData(listApiKey, lastHours, page, inMinutes, dateRange, null, null, null);
    }

    public Page<NotificationSummary> aggregatedData(List<String> listApiKey, int lastHours, @Valid Pageable page, boolean inMinutes) {
        return aggregatedData(listApiKey, lastHours, page, inMinutes, Arrays.asList(), null, null, null);
    }

    public Page<NotificationSummary> aggregatedData(List<String> listApiKey, int lastHours, @Valid Pageable page, boolean inMinutes,
                                                    @Valid List<Long> dateRange, String parameter, String condition, Double searchValue) {
        //        MongoClient mongoClient = MongoClients.create(getUrl());
        MongoDatabase mongoDatabase = mongoClient.getDatabase(this.database);
        MongoCollection<Document> mongoCollection = mongoDatabase.getCollection(collection);

        final long dateTimeNow = (new Date()).getTime();
        final int offsetMinutes = 0;
        Date dateStart;
        Date dateEnd;

        if (dateRange.size() == 2) {
            dateStart = new Date(dateRange.get(0));
            dateEnd = new Date(dateRange.get(1));
        } else {
            dateStart =
                    new Date(dateTimeNow - (inMinutes ? TimeUnit.MINUTES.toMillis(lastHours) : TimeUnit.HOURS.toMillis(lastHours)) - TimeUnit.MINUTES.toMillis(offsetMinutes + 1));
            dateEnd = new Date(dateTimeNow - TimeUnit.MINUTES.toMillis(offsetMinutes));
        }

        // @formatter:off
        List<Document> listAgg = new ArrayList<>();
        Document document = new Document("data_timestamp", new Document("$gte", dateStart).append("$lt", dateEnd))
                .append("sensor_key", new Document("$in", listApiKey));

        if(parameter!=null && Sensor.isSensorProperty(parameter)){
            String logicalOperator;

            switch (condition){
                case "less_than": logicalOperator="$lt";break;
                case "greater_than": logicalOperator="$gt";break;
                default: logicalOperator="$eq";
            }

            document.append("threshold_property", new Document("$eq", parameter));
            document.append("threshold_value", new Document(logicalOperator, searchValue));
        }

        listAgg.add(new Document("$match", document));

        listAgg.add(new Document("$sort", new Document("data_timestamp", -1L)));

        listAgg.add(new Document("$group", new Document("_id", new Document("sensor_key", "$sensor_key")
                    .append("description", "$description"))
//                .append("id", new Document("$first", new Document("$toLong", "$timestamp")))
                .append("sensor_key", new Document("$first", "$sensor_key"))
                .append("label", new Document("$first", "$label"))
                .append("description", new Document("$first", "$description"))
                .append("threshold_property", new Document("$first", "$threshold_property"))
                .append("threshold_boundary", new Document("$first", "$threshold_boundary"))
                .append("threshold_value", new Document("$first", "$threshold_value"))
                .append("last_id", new Document("$first", "$_id"))
                .append("start_date", new Document("$last", "$data_timestamp"))
                .append("end_date", new Document("$first", "$data_timestamp"))
                .append("timestamp", new Document("$first", "$timestamp"))
                .append("data_value", new Document("$first", "$data_value"))
                .append("is_read", new Document("$first", "$is_read"))
                .append("count", new Document("$sum", 1L))));

        listAgg.add(new Document("$setWindowFields", new Document("partitionBy", new BsonNull())
                .append("sortBy", new Document("timestamp", 1L))
                .append("output", new Document("id", new Document("$documentNumber", new Document())))));

        listAgg.add(new Document("$skip", (page.getPageNumber()) * page.getPageSize()));

        listAgg.add(new Document("$limit", page.getPageSize()));

        listAgg.add(new Document("$sort", new Document("id", -1L)));

        listAgg.add(new Document("$project", new Document("id", 1L)
                .append("_id", 0L)
                .append("sensor_key", 1L)
                .append("label", 1L)
                .append("description", 1L)
                .append("start_date", new Document("$dateToString", new Document("format", "%Y-%m-%dT%H:%M:%S")
                        .append("date", "$start_date")))
                .append("end_date", new Document("$dateToString", new Document("format", "%Y-%m-%dT%H:%M:%S")
                        .append("date", "$end_date")))
                .append("timestamp", new Document("$dateToString", new Document("format", "%Y-%m-%dT%H:%M:%S")
                        .append("date", "$timestamp")))
                .append("data_value", 1L)
                .append("threshold_property", 1L)
                .append("threshold_boundary", 1L)
                .append("threshold_value", 1L)
                .append("is_read", 1L)
                .append("count", 1L)
                .append("last_id", 1L)));

        // @formatter:on

        return getNotificationSummaryListAsPage(mongoCollection, listAgg, page);
    }

    private Page<NotificationSummary> getNotificationSummaryListAsPage(MongoCollection<Document> mongoCollection, List<Document> listAgg,
                                                                       Pageable page) {
        AggregateIterable<Document> documentAggregateIterable = mongoCollection.aggregate(listAgg);

        ObjectMapper mapper = new ObjectMapper();
        List<NotificationSummary> notificationSummaryList = new ArrayList<>();

        for (Iterator<Document> documentIterator = documentAggregateIterable.iterator(); documentIterator.hasNext(); ) {
            Document document = documentIterator.next();
            try {
                notificationSummaryList.add(mapper.readValue(document.toJson(), NotificationSummary.class));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }

        //        return notificationSummaryList;
        Page<NotificationSummary> notificationSummaryPage = new Page<NotificationSummary>() {
            long count = notificationSummaryList.stream().count();

            @Override
            public int getTotalPages() {
                return (count != 0) ? 1 : 0;
            }

            @Override
            public long getTotalElements() {
                return count;
            }

            @Override
            public <U> Page<U> map(Function<? super NotificationSummary, ? extends U> converter) {
                return null;
            }

            @Override
            public int getNumber() {
                return page.getPageNumber();
            }

            @Override
            public int getSize() {
                return (count != 0) ? (int) count : page.getPageSize();
            }

            @Override
            public int getNumberOfElements() {
                return (int) count;
            }

            @Override
            public List<NotificationSummary> getContent() {
                return notificationSummaryList;
            }

            @Override
            public boolean hasContent() {
                return (count != 0);
            }

            @Override
            public Sort getSort() {
                return Sort.sort(NotificationSummary.class);
            }

            @Override
            public boolean isFirst() {
                return true;
            }

            @Override
            public boolean isLast() {
                return true;
            }

            @Override
            public boolean hasNext() {
                return false;
            }

            @Override
            public boolean hasPrevious() {
                return false;
            }

            @Override
            public Pageable nextPageable() {
                return null;
            }

            @Override
            public Pageable previousPageable() {
                return null;
            }

            @NotNull
            @Override
            public Iterator<NotificationSummary> iterator() {
                return notificationSummaryList.iterator();
            }
        };

        return notificationSummaryPage;
    }


    @NotNull
    private String getUrl() {
        String url = "mongodb://" + username + ":" + password + "@" + host + ":" + port + "/" + database + "?authSource=admin&readPreference" +
                "=primary&appname=MongoDB%20Compass&ssl=false";
        return url;
    }
}
