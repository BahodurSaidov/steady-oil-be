package com.steadyoil.mqtt.service;

import com.steadyoil.mqtt.domain.SensorData;
import com.steadyoil.mqtt.repository.SensorDataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
public class SensorDataService {
    @Autowired
    private SensorDataRepository sensorDataRepository;

    @Autowired
    private SequenceGeneratorService sequenceGeneratorService;

    @Transactional
    @Async
    public void createSensorDataAsync(SensorData sensorData) {
                sensorData.setId(sequenceGeneratorService.generateSequence(SensorData.SEQUENCE_NAME));
                sensorData.setTimestamp(new Date());
        sensorDataRepository.save(sensorData);
    }
}
