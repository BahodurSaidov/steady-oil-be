package com.steadyoil.mqtt.domain;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Setter
@Document(collection = "#{@environment.getProperty('com.steadyoil.mongodb.collections.sequences')}")
public class DatabaseSequence {
    @Id
    private String id;
    private long seq;
}
