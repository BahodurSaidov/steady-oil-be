package com.steadyoil.mqtt.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
public class SensorDataSummary {
    @JsonProperty("timestamps")
    private List<Date> timestamps;
    @JsonProperty("key")
    private String key;
    @JsonProperty("air_temperature")
    private SensorDataMinMaxVal airTemperature;
    @JsonProperty("water_temperature")
    private SensorDataMinMaxVal waterTemperature;
    @JsonProperty("compressor_temperature")
    private SensorDataMinMaxVal compressorTemperature;
    @JsonProperty("liquid_pressure")
    private SensorDataMinMaxVal liquidPressure;
    @JsonProperty("suction_pressure")
    private SensorDataMinMaxVal suctionPressure;
    @JsonProperty("water_flow")
    private SensorDataMinMaxVal waterFlow;

}
