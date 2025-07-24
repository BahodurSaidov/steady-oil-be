package com.steadyoil.mqtt.service;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class MqttSensorCmdDTO {
    private String key;
    private String cmd;

    @Override
    public String toString() {
        return "{\"key\":\"" + key + "\",\"cmd\":\"" + cmd + "\"}";
    }
}
