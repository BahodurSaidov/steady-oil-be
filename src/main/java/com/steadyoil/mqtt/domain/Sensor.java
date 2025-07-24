package com.steadyoil.mqtt.domain;

import com.steadyoil.mqtt.util.DomainConstants;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.Date;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "#{@environment.getProperty('com.steadyoil.mongodb.collections.sensors')}")
public class Sensor {
    @Transient
    public static final String SEQUENCE_NAME = "sensors_sequence";
    @Id
    private long id;

    private String key;
    @NotBlank
    private String label;
    @NotBlank
    private String mac;
    @NotBlank
    private String description;
    @Valid
    @Field("geo_location")
    private GeoLocation geoLocation;

    private Date timestamp;

    private String status = DomainConstants.SENSOR_STATUS_OFF;
    private String production = DomainConstants.SENSOR_STATUS_OFF;


    private String activity = DomainConstants.SENSOR_ACTIVITY_INACTIVE;

    @Valid
    private Thresholds thresholds;

    public static boolean isSensorProperty(String test) {
        for (SensorProperty p : SensorProperty.values()) {
            if (p.name().equals(test)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public String toString() {
        return "Sensor{" + "id=" + id + ", key='" + key + '\'' + ", label='" + label + '\'' + ", mac='" + mac + '\'' + ", description='" + description + '\'' + ", geoLocation=" + geoLocation + ", timestamp=" + timestamp + ", status='" + status + '\'' + ", production='" + production + '\'' + ", activity='" + activity + '\'' + ", thresholds=" + thresholds + '}';
    }

    public enum SensorProperty {
        air_temperature, water_temperature, compressor_temperature, liquid_pressure, suction_pressure, water_flow;
    }

    @AllArgsConstructor
    @Getter
    @Setter
    private static class GeoLocation {
        @NotNull
        private Double latitude = 0.0;
        @NotNull
        private Double longitude = 0.0;

        @Override
        public String toString() {
            return "{" + "latitude=" + latitude + ", longitude=" + longitude + '}';
        }
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    @AllArgsConstructor
    @Getter
    @Setter
    public static class Thresholds {
        @Valid
        @Field("air_temperature")
        private Threshold airTemperature;
        @Valid
        @Field("water_temperature")
        private Threshold waterTemperature;
        @Valid
        @Field("compressor_temperature")
        private Threshold compressorTemperature;
        @Valid
        @Field("liquid_pressure")
        private Threshold liquidPressure;
        @Valid
        @Field("suction_pressure")
        private Threshold suctionPressure;
        @Valid
        @Field("water_flow")
        private Threshold waterFlow;

        @Override
        public String toString() {
            return "Thresholds{" + "airTemperature=" + airTemperature + ", waterTemperature=" + waterTemperature + ", compressorTemperature=" + compressorTemperature + ", liquidPressure=" + liquidPressure + ", suctionPressure=" + suctionPressure + ", waterFlow=" + waterFlow + '}';
        }
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Getter
    @Setter
    public static class Threshold {
        @NotNull
        private Double low = 0.0;
        @NotNull
        private Double high = 0.0;
        private boolean enabled = false;
        private boolean mail = false;

        @Override
        public String toString() {
            return "Threshold{" + "low=" + low + ", high=" + high + ", enabled=" + enabled + '}';
        }
    }

}
