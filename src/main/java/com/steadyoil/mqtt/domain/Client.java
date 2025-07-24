package com.steadyoil.mqtt.domain;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "#{@environment.getProperty('com.steadyoil.mongodb.collections.clients')}")
public class Client {
    @Transient
    public static final String SEQUENCE_NAME = "clients_sequence";
    @Id
    private long id;

    @NotBlank
    private String label;
    @NotBlank
    private String person;
    @NotBlank
    private String address;
    @NotBlank
    private String mobile;
    @NotBlank
    private String description;
    @Valid
    private List<Long> sensors;
    @Valid
    private List<String> emails;

    private Date timestamp;


    @Override
    public String toString() {
        return "Client{" + "id=" + id + ", label='" + label + '\'' + ", person='" + person + '\'' + ", address='" + address + '\'' + ", mobile='" + mobile + '\'' + ", description='" + description + '\'' + ", sensors=" + sensors + ", emails=" + emails + ", timestamp=" + timestamp + '}';
    }
}
