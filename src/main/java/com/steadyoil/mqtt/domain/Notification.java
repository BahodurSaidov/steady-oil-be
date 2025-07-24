package com.steadyoil.mqtt.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Date;

@CommonsLog
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "#{@environment.getProperty('com.steadyoil.mongodb.collections.notifications')}")
public class Notification {
    @Transient
    public static final String SEQUENCE_NAME = "notifications_sequence";

    @Id
    private long id;

    @NotBlank
    @Field("sensor_key")
    private String sensorKey;

    @NotBlank
    private String label;

    @NotBlank
    private String description;

    private Date timestamp;

    @JsonProperty("is_read")
    @Field("is_read")
    private boolean isRead = false;

    @NotNull
    @Field("data_id")
    private long dataId;

    @NotNull
    @Field("data_value")
    private Double dataValue;

    @NotNull
    @Field("data_timestamp")
    private Date dataTimestamp;


    @NotNull
    @Field("threshold_property")
    private String thresholdProperty;

    @NotNull
    @Field("threshold_boundary")
    private String thresholdBoundary;

    @NotNull
    @Field("threshold_value")
    private Double thresholdValue;

    @Override
    public String toString() {
        return "Notification{" + "id=" + id + ", sensorKey='" + sensorKey + '\'' + ", label='" + label + '\'' + ", description='" + description + '\'' + ", timestamp=" + timestamp + ", isRead=" + isRead + ", dataId=" + dataId + ", dataValue=" + dataValue + ", dataTimestamp=" + dataTimestamp + ", thresholdProperty='" + thresholdProperty + '\'' + ", thresholdBoundary='" + thresholdBoundary + '\'' + ", thresholdValue=" + thresholdValue + '}';
    }


    public enum ThresholdBoundary {
        low, high;
    }
}
