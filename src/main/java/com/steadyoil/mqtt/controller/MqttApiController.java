package com.steadyoil.mqtt.controller;

import com.steadyoil.mqtt.service.MqttSensorCommands;
import com.steadyoil.mqtt.service.MqttService;
import com.steadyoil.mqtt.service.SensorService;
import com.steadyoil.mqtt.util.DomainConstants;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "${com.steadyoil.cors.url}")
@RequestMapping("/api/v1/mqtt")
@RestController
public class MqttApiController {
    private final SensorService sensorService;
    private final MqttService mqttService;

    MqttApiController(SensorService sensorService, MqttService mqttService) {
        this.sensorService = sensorService;
        this.mqttService = mqttService;
    }

    /**
     * Set sensor production status and publish mqtt message.
     *
     * @param cmd
     * @return
     */
    @PostMapping("/cmd")
    public String production(@RequestBody @Valid Payload cmd) {
        System.out.println(cmd.sensorId + " " + cmd.status);
        if (cmd.status.equals(DomainConstants.SENSOR_STATUS_OFF)) {
            mqttService.publishCmd(cmd.sensorId, MqttSensorCommands.STATUS_OFF.getCmd());
            sensorService.updateSensorProduction(DomainConstants.SENSOR_STATUS_OFF, cmd.sensorId);
            sensorService.updateSensorStatus(DomainConstants.SENSOR_STATUS_OFF, cmd.sensorId);
        } else {
            mqttService.publishCmd(cmd.sensorId, MqttSensorCommands.STATUS_ON.getCmd());
            sensorService.updateSensorProduction(DomainConstants.SENSOR_STATUS_ON, cmd.sensorId);
            sensorService.updateSensorStatus(DomainConstants.SENSOR_STATUS_ON, cmd.sensorId);
        }
        return "Done";
    }


    /**
     * Payload for getting json.
     */
    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    private static class Payload {
        // {"sensor_id":1,"status":"on"}
        @NotNull
        @JsonProperty("sensor_id")
        private long sensorId;
        @NotEmpty
        @Size(min = 2, max = 3)
        @JsonProperty("status")
        private String status;
    }
}
