package com.steadyoil.mqtt.service;

import com.steadyoil.mqtt.domain.SensorData;
import com.steadyoil.mqtt.dto.SensorDataBaseShort;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.integration.mqtt.event.MqttConnectionFailedEvent;
import org.springframework.messaging.MessageHeaders;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;

@CommonsLog
@Service
public class MqttInboundService {
    @Autowired
    private SensorDataService sensorDataService;

    @Autowired
    private NotificationService notificationService;

    private ObjectMapper objectMapper = new ObjectMapper();

    @EventListener(MqttConnectionFailedEvent.class)
    public void logConnectionError(MqttConnectionFailedEvent event) {
        log.error("MQTT Connection failed: " + event.toString());

    }

    @Async
    public void messageArrived(String msg, MessageHeaders headers) {
        log.info(String.format("MQTT Received message: %s from topic: %s", msg, headers.get("mqtt_receivedTopic")));
        Map<String, String> msgObj;
        try {
            msgObj = objectMapper.readValue(msg, Map.class);
        } catch (Exception e) {
            log.error("Error parsing message: " + msg, e);
            return;
        }
        SensorDataBaseShort sensorDataMsg = objectMapper.convertValue(msgObj, SensorDataBaseShort.class);
        SensorData sensorData = new SensorData(sensorDataMsg);
        sensorDataService.createSensorDataAsync(sensorData);
        notificationService.createNotificationAsync(sensorData);
    }
}