package com.steadyoil.mqtt.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class SensorDataBaseShort {
    @JsonProperty("key")
    @NotBlank
    private String key;
    @NotNull
    @JsonProperty("A")
    private Double airTemperature;
    @NotNull
    @JsonProperty("W")
    private Double waterTemperature;
    @NotNull
    @JsonProperty("C")
    private Double compressorTemperature;
    @NotNull
    @JsonProperty("L")
    private Double liquidPressure;
    @NotNull
    @JsonProperty("S")
    private Double suctionPressure;
    @NotNull
    @JsonProperty("F")
    private Double waterFlow;
    private Date timestamp;
}
