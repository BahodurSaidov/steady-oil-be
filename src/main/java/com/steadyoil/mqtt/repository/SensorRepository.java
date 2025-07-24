package com.steadyoil.mqtt.repository;

import com.steadyoil.mqtt.domain.Sensor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface SensorRepository extends MongoRepository<Sensor, Long> {
    Page<Sensor> findAll(Pageable page);

    Page<Sensor> findAllByIdIn(List<Long> idList, Pageable page);
}
