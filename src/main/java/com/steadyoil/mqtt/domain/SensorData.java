package com.steadyoil.mqtt.domain;

import com.steadyoil.mqtt.dto.SensorDataBaseShort;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "#{@environment.getProperty('com.steadyoil.mongodb.collections.data_in')}")
@NoArgsConstructor
public class SensorData extends SensorDataBase {
    @Transient
    public static final String SEQUENCE_NAME = "sensors_data_sequence";

    public SensorData(SensorDataBaseShort sensorDataShort) {
        this.setKey(sensorDataShort.getKey());

        this.setAirTemperature(sensorDataShort.getAirTemperature());
        this.setCompressorTemperature(sensorDataShort.getCompressorTemperature());
        this.setWaterTemperature(sensorDataShort.getWaterTemperature());
        this.setLiquidPressure(sensorDataShort.getLiquidPressure());
        this.setSuctionPressure(sensorDataShort.getSuctionPressure());
        this.setWaterFlow(sensorDataShort.getWaterFlow());
    }
}
