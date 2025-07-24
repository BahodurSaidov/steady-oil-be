package com.steadyoil.mqtt.repository;

import com.steadyoil.mqtt.domain.SensorData;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Date;
import java.util.List;

public interface SensorDataRepository extends MongoRepository<SensorData, Long> {
    Page<SensorData> findByKeyInAndTimestampAfter(List<@NotBlank String> keys, Date timestamp, Pageable pageable);

    long countByKeyAndTimestampAfter(String key, Date timestamp);

    SensorData findFirstByKeyOrderByIdDesc(String key);
}
