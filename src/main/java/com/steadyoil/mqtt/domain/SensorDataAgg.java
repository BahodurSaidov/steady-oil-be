package com.steadyoil.mqtt.domain;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Getter
@Setter
@Document(collection = "#{@environment.getProperty('com.steadyoil.mongodb.collections.data_agg_1_min')}")
public class SensorDataAgg extends SensorDataBase {
    @Transient
    public static final String SEQUENCE_NAME = "sensors_data_agg_sequence";

    @NotNull
    @Field("last_id")
    private long lastId;
}
