package com.steadyoil.mqtt.service;

import com.steadyoil.mqtt.domain.Client;
import com.steadyoil.mqtt.domain.Sensor;
import com.steadyoil.mqtt.repository.ClientRepository;
import com.steadyoil.mqtt.repository.SensorRepository;
import com.steadyoil.mqtt.util.AESUtil;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@CommonsLog
@Service
public class SensorService {
    @Autowired
    private SequenceGeneratorService sequenceGeneratorService;

    @Autowired
    private SensorRepository sensorRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Transactional
    public Sensor createSensor(Sensor sensor) {
        sensor.setId(sequenceGeneratorService.generateSequence(Sensor.SEQUENCE_NAME));
        sensor.setKey(AESUtil.getApiKey(String.valueOf(sensor.getId())));
        sensor.setTimestamp(new Date());
        return sensorRepository.save(sensor);
    }

    @Transactional
    public Sensor updateSensor(Sensor sensor) {
        Optional<Sensor> sensorDb = this.sensorRepository.findById(sensor.getId());

        if (sensorDb.isPresent()) {
            Sensor sensorUpdate = sensorDb.get();
            sensorUpdate.setLabel(sensor.getLabel());
            sensorUpdate.setDescription(sensor.getDescription());
            sensorUpdate.setMac(sensor.getMac());
            sensorUpdate.setGeoLocation(sensor.getGeoLocation());
            sensorRepository.save(sensorUpdate);
            return sensorUpdate;
        } else {
            throw new Error("Record not found with id : " + sensor.getId());
        }
    }

    public Page<Sensor> getAllSensor(Pageable page) {
        return this.sensorRepository.findAll(page);
    }

    public List<Sensor> getSensors() {
        return this.sensorRepository.findAll();
    }

    public Page<Sensor> getAllSensorByIdIn(List<Long> idList, Pageable page) {
        return this.sensorRepository.findAllByIdIn(idList, page);
    }

    @Transactional
    public void deleteSensor(long sensorId) {
        Optional<Sensor> sensorDb = this.sensorRepository.findById(sensorId);

        if (sensorDb.isPresent()) {
            this.sensorRepository.delete(sensorDb.get());
        } else {
            log.error("Record for delete not found with id : " + sensorId);
        }

    }

    @Transactional
    public void updateSensorProduction(String status, long sensorId) {
        Optional<Sensor> sensorDb = this.sensorRepository.findById(sensorId);

        if (sensorDb.isPresent()) {
            Sensor sensorUpdate = sensorDb.get();
            sensorUpdate.setProduction(status);
            sensorRepository.save(sensorUpdate);
        } else {
            log.error("Record for update not found with id : " + sensorId);
        }
    }

    @Transactional
    public void updateSensorStatus(String status, long sensorId) {
        Optional<Sensor> sensorDb = this.sensorRepository.findById(sensorId);

        if (sensorDb.isPresent()) {
            Sensor sensorUpdate = sensorDb.get();
            sensorUpdate.setStatus(status);
            sensorRepository.save(sensorUpdate);
        } else {
            log.error("Record for update not found with id : " + sensorId);
        }
    }

    @Transactional
    public Sensor updateSensorThresholds(long id, Sensor.Thresholds thresholds) {
        Sensor sensor = getSensorById(id);
        sensor.setThresholds(thresholds);
        sensorRepository.save(sensor);
        return sensor;
    }

    public Sensor getSensorById(long sensorId) {

        Optional<Sensor> sensorDb = this.sensorRepository.findById(sensorId);

        if (sensorDb.isPresent()) {
            return sensorDb.get();
        } else {
            throw new Error("Record not found with id : " + sensorId);
        }
    }

    @Transactional
    public void updateSave(Sensor sensor) {
        sensorRepository.save(sensor);
    }

    public List<String> getSensorClients(long id) {
        return clientRepository.findBySensorsContains(id).stream().map(c -> {
            return c.getLabel();
        }).collect(Collectors.toList());
    }

    public Client getClientBySensorId(long sensorId) {
        return clientRepository.findFirstBySensorsContains(sensorId);
    }
}
