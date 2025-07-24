package com.steadyoil.mqtt.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Date;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public class NotificationSummary {

    @JsonProperty("id")
    private long id;

    @JsonProperty("last_id")
    private long lastId;

    @JsonProperty("threshold_value")
    private Double thresholdValue;

    @JsonProperty("data_value")
    private Double dataValue;

    @JsonProperty("count")
    private long count;

    @JsonProperty("sensor_key")
    private String sensorKey;

    @JsonProperty("label")
    private String label;

    @JsonProperty("description")
    private String description;

    @JsonProperty("threshold_property")
    private String thresholdProperty;

    @JsonProperty("threshold_boundary")
    private String thresholdBoundary;

    @JsonProperty("start_date")
    private Date startDate;

    @JsonProperty("end_date")
    private Date endDate;

    @JsonProperty("timestamp")
    private Date timestamp;

    @JsonProperty("is_read")
    private boolean isRead;

}
