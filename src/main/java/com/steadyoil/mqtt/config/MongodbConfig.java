package com.steadyoil.mqtt.config;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;

import java.util.Collection;
import java.util.Collections;

@Configuration
public class MongodbConfig extends AbstractMongoClientConfiguration {

    @Value("${com.steadyoil.mongodb.host}")
    private String host;

    @Value("${com.steadyoil.mongodb.port}")
    private String port;
    @Value("${com.steadyoil.mongodb.database}")
    private String database;

    @Value("${com.steadyoil.mongodb.username}")
    private String username;

    @Value("${com.steadyoil.mongodb.password}")
    private String password;

    @Override
    @Bean
    public MongoClient mongoClient() {
        ConnectionString connectionString =
                new ConnectionString("mongodb://" + username + ":" + password + "@" + host + ":" + port + "/" + this.getDatabaseName() +
                        "?authSource=admin&readPreference=primary&appname=MongoDB%20Compass&ssl=false");
        //        mongodb://admin:secret@localhost:27017/?authSource=admin&readPreference=primary&appname=MongoDB%20Compass&ssl=false
        //        mongodb://admin:secret@localhost:27017/sensors
        //        System.out.println(connectionString);
        MongoClientSettings mongoClientSettings = MongoClientSettings.builder().applyConnectionString(connectionString).build();

        return MongoClients.create(mongoClientSettings);
    }

    @Override
    protected String getDatabaseName() {
        return database;
    }

    @Override
    public Collection getMappingBasePackages() {
        return Collections.singleton("com.steadyoil.producer");
    }
}
