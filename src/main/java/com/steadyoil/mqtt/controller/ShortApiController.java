package com.steadyoil.mqtt.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = "${com.steadyoil.cors.url}")
@RequestMapping("/v1")
@RestController
public class ShortApiController {
//    @Autowired
//    private KafkaProducerService kafkaProducerService;


    /**
     * Send given sensor data to kafka topics.
     *
     * @param sensorParams
     * @return String
     */
//    @GetMapping("/in")
    //    public String acceptData(@RequestParam Map<String, String> sensorParams) {
    //        ObjectMapper objectMapper = new ObjectMapper();
    //        SensorDataBaseShort sensorDataShort = objectMapper.convertValue(sensorParams, SensorDataBaseShort.class);
    //
    //        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    //        Set<ConstraintViolation<SensorDataBaseShort>> errors = validator.validate(sensorDataShort);
    //
    //        if (!errors.isEmpty()) {
    //            throw new ConstraintViolationException(errors);
    //        }
    //
    //        SensorData sensorData = new SensorData(sensorDataShort);
    //
    //        kafkaProducerService.produce(sensorDataShort.getKey(), sensorData);
    //        return "Accepted";
    //    }

}
