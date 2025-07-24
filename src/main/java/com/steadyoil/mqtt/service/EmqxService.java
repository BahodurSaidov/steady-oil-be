package com.steadyoil.mqtt.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.Map;

@Service
public class EmqxService {
    private final RestTemplate restTemplate;

    @Value("${mqtt.broker.api_url}")
    private String apiUrl;

    @Value("${mqtt.broker.api_token}")
    private String apiToken;

    @Value("${mqtt.broker.api_secret}")
    private String apiSecret;

    @Autowired
    public EmqxService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }


    public Map<String, Object> getClientDetails(String clientId) {
        String url = apiUrl + "clients/" + clientId;
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        String auth = apiToken + ":" + apiSecret;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
        headers.set("Authorization", "Basic " + encodedAuth);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
            if (response.getStatusCode() == HttpStatus.OK) {
                return response.getBody();
            } else {
                throw new RuntimeException("Unexpected response status: " + response.getStatusCode());
            }
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return null;
            } else {
                throw new RuntimeException("Failed to get client details from EMQX server", e);
            }
        }
    }
}
