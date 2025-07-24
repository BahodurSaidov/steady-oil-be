package com.steadyoil.mqtt.repository;

import com.steadyoil.mqtt.domain.Notification;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Date;
import java.util.List;

public interface NotificationRepository extends MongoRepository<Notification, Long> {
    Page<Notification> findBySensorKeyInAndTimestampAfter(List<@NotBlank String> keys, Date timestamp, Pageable pageable);
}
