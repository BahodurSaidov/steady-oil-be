package com.steadyoil.mqtt.service;

import com.steadyoil.mqtt.util.AESUtil;
import jakarta.validation.constraints.NotNull;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.MessageChannel;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@CommonsLog
@Service
public class MqttService {
    @Autowired
    private MessageChannel mqttOutboundChannel;
    @Value("${mqtt.retain.publish}")
    private boolean mqttRetainPublish;
    @Value("${mqtt.topic.publish}")
    private String mqttTopicPublish;
    @Value("${mqtt.qos.publish}")
    private int mqttQosPublish;


    /**
     * Publish MQTT message
     *
     * @param id  Client ID
     * @param cmd Command
     */
    @Async
    public void publishCmd(@NotNull long id, String cmd) {
        String key = AESUtil.getApiKey(String.valueOf(id));
        MqttSensorCmdDTO mqttSensorCmdDTO = new MqttSensorCmdDTO(key, cmd);
        log.info(String.format("MQTT Publishing command: %s", mqttSensorCmdDTO));
        mqttOutboundChannel.send(MessageBuilder.withPayload(mqttSensorCmdDTO.toString()).setHeader(MqttHeaders.TOPIC, mqttTopicPublish).setHeader(MqttHeaders.QOS, mqttQosPublish).setHeader(MqttHeaders.RETAINED, mqttRetainPublish).build());
    }

}