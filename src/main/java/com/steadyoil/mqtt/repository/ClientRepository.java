package com.steadyoil.mqtt.repository;

import com.steadyoil.mqtt.domain.Client;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ClientRepository extends MongoRepository<Client, Long> {
    Page<Client> findAll(Pageable page);

    List<Client> findBySensorsContains(long sensorId);

    Client findFirstBySensorsContains(long sensorId);
}
