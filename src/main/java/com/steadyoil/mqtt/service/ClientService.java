package com.steadyoil.mqtt.service;

import com.steadyoil.mqtt.domain.Client;
import com.steadyoil.mqtt.domain.Sensor;
import com.steadyoil.mqtt.domain.SensorDataAgg;
import com.steadyoil.mqtt.repository.ClientRepository;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@CommonsLog
@Service
public class ClientService {
    //    private final KafkaListenerService kafkaListenerService;

    private final ClientRepository clientRepository;

    private final SensorService sensorService;

    private final MailService mailService;
    private final SequenceGeneratorService sequenceGeneratorService;
    private final SensorAggregationService sensorAggregationService;
    @Value("${com.steadyoil.client.id}")
    private long clientId;
    @Value("${com.steadyoil.client.saveAgg.lastMin}")
    private int minLastMinutes;
    @Value("${com.steadyoil.client.saveAgg.precisionMin}")
    private int precisionMinutes;

    ClientService(ClientRepository clientRepository, SensorService sensorService, MailService mailService,
                  SequenceGeneratorService sequenceGeneratorService, SensorAggregationService sensorAggregationService) {
        //        this.kafkaListenerService = kafkaListenerService;
        this.clientRepository = clientRepository;
        this.sensorService = sensorService;
        this.mailService = mailService;
        this.sequenceGeneratorService = sequenceGeneratorService;
        this.sensorAggregationService = sensorAggregationService;
    }

    //    @EventListener(ApplicationReadyEvent.class)
    //    public void registerTopicsAfterStartup() {
    //        try {
    //            //            Client client = this.clientRepository.findById(clientId).orElseThrow(() -> {
    //            //                return new ValidateException("ApplicationReadyEvent: Client id not found: " + clientId);
    //            //            });
    //            Set<String> sensorsString = new HashSet<>();
    //            List<Client> clientList = clientRepository.findAll();
    //            for (int i = 0; i < clientList.size(); i++) {
    //                List<Long> sensors = clientList.get(i).getSensors();
    //                for (int index = 0; index < sensors.size(); index++) {
    //                    sensorsString.add(topicBase + String.valueOf(sensors.get(index)));
    //                }
    //            }
    //
    //            kafkaListenerService.registerListener(sensorsString);
    //        } catch (Exception e) {
    //            log.error(e.getMessage());
    //        }
    //    }

    @Transactional
    public Client createClient(Client client) {
        client.setId(sequenceGeneratorService.generateSequence(Client.SEQUENCE_NAME));
        client.setTimestamp(new Date());
        return clientRepository.save(client);
    }

    @Transactional
    public Client updateClient(Client client) {
        Optional<Client> clientDb = this.clientRepository.findById(client.getId());

        if (clientDb.isPresent()) {
            Client clientUpdate = clientDb.get();
            clientUpdate.setLabel(client.getLabel());
            clientUpdate.setDescription(client.getDescription());
            clientUpdate.setAddress(client.getAddress());
            clientUpdate.setMobile(client.getMobile());
            clientUpdate.setPerson(client.getPerson());
            clientUpdate.setSensors(client.getSensors());
            clientUpdate.setEmails(client.getEmails());
            clientRepository.save(clientUpdate);
            return clientUpdate;
        } else {
            throw new Error("Record not found with id : " + client.getId());
        }
    }

    public Page<Client> getAllClient(Pageable page) {
        return this.clientRepository.findAll(page);
    }

    @Transactional
    public void deleteClient(long clientId) {
        Optional<Client> clientDb = this.clientRepository.findById(clientId);

        if (clientDb.isPresent()) {
            this.clientRepository.delete(clientDb.get());
        } else {
            throw new Error("Record not found with id : " + clientId);
        }

    }

    public Client getCurrentClient() {
        return getClientById(clientId);
    }

    public Client getClientById(long clientId) {

        Optional<Client> clientDb = this.clientRepository.findById(clientId);

        if (clientDb.isPresent()) {
            return clientDb.get();
        } else {
            throw new Error("Record not found with id : " + clientId);
        }
    }

    @Scheduled(fixedRateString = "${com.steadyoil.mail.statusCheckMs}", initialDelay = 10000)
    public void checkSensorsStatusSchedule() {
        try {
            log.info("Starting checkSensorsStatusSchedule...");
            List<Client> clientList = getAllClients();
            for (Client client : clientList) {
                final List<Long> sensorIdList = client.getSensors();
                if (sensorIdList == null) {continue;}
                for (long sensorId : sensorIdList) {
                    mailService.checkStatusAndSendMail(sensorId, client.getEmails(), client.getLabel());
                }
            }
            log.info("Finished checkSensorsStatusSchedule...");
        } catch (Exception e) {
            log.error("Schedule error: " + e.getMessage());
        }
    }

    public List<Client> getAllClients() {
        return this.clientRepository.findAll();
    }

    @Scheduled(fixedRateString = "${com.steadyoil.mail.thresholdCheckMs}", initialDelay = 10000)
    public void checkRecentThresholdSchedule() {
        try {
            log.info("Starting checkRecentThresholdSchedule...");
            List<Client> clientList = getAllClients();
            for (Client client : clientList) {
                final List<Long> sensorIdList = client.getSensors();
                if (sensorIdList == null) {continue;}
                for (long sensorId : sensorIdList) {
                    mailService.checkThresholdAndSendMail(sensorId, client.getEmails(), client.getLabel());
                }
            }
            log.info("Finished checkRecentThresholdSchedule...");
        } catch (Exception e) {
            log.error("Schedule error: " + e.getMessage());
        }
    }

    @Scheduled(fixedRateString = "${com.steadyoil.client.saveAgg.ms}", initialDelay = 10000)
    public void saveAggDataSchedule() {
        try {
            log.info("Starting saveAggDataSchedule...");
            List<Sensor> sensorList = sensorService.getSensors();
            for (Sensor sensor : sensorList) {
                List<SensorDataAgg> sensorDataAggList = sensorAggregationService.aggregatedData3(sensor.getKey(), minLastMinutes, precisionMinutes);
                if (!sensorDataAggList.isEmpty()) {
                    sensorAggregationService.saveAggDataList(sensorDataAggList);
                }
            }
            log.info("Finished saveAggDataSchedule...");
        } catch (Exception e) {
            log.error("Schedule error: " + e.getMessage());
        }
    }
}
