package com.steadyoil.mqtt.repository;

import com.steadyoil.mqtt.domain.SensorDataAgg;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SensorDataAggRepository extends MongoRepository<SensorDataAgg, Long> {
    SensorDataAgg findFirstByKeyOrderByLastIdDesc(String key);

    SensorDataAgg findFirstByOrderByIdDesc();
}
