package com.steadyoil.mqtt.domain;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Date;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SensorDataBase {
    @Id
    private long id;
    @NotBlank
    private String key;
    @NotNull
    @Field("air_temperature")
    private Double airTemperature;
    @NotNull
    @Field("water_temperature")
    private Double waterTemperature;
    @NotNull
    @Field("compressor_temperature")
    private Double compressorTemperature;
    @NotNull
    @Field("liquid_pressure")
    private Double liquidPressure;
    @NotNull
    @Field("suction_pressure")
    private Double suctionPressure;
    @NotNull
    @Field("water_flow")
    private Double waterFlow;
    private Date timestamp;


    @Override
    public String toString() {
        return "SensorData{" + "key='" + key + '\'' + ", airTemperature=" + airTemperature + ", waterTemperature=" + waterTemperature + ", " +
                "compressorTemperature=" + compressorTemperature + ", liquidPressure=" + liquidPressure + ", suctionPressure=" + suctionPressure +
                ", waterFlow=" + waterFlow + ", timestamp=" + timestamp + '}';
    }
}
