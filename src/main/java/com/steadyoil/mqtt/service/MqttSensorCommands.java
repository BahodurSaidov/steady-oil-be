package com.steadyoil.mqtt.service;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MqttSensorCommands {
    STATUS_OFF("status-off"), STATUS_ON("status-on"), STATUS_RESTART("status-restart");
    private final String cmd;
}