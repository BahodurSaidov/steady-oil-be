package com.steadyoil.mqtt.controller;

import com.steadyoil.mqtt.domain.SensorData;
import com.steadyoil.mqtt.dto.SensorDataSummary;
import com.steadyoil.mqtt.repository.SensorDataRepository;
import com.steadyoil.mqtt.service.MailService;
import com.steadyoil.mqtt.service.SensorAggregationService;
import com.steadyoil.mqtt.util.AESUtil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@CrossOrigin(origins = "${com.steadyoil.cors.url}")
@RequestMapping("/api/v1/data")
@RestController
public class DataApiController {
//    @Autowired
//    private KafkaProducerService kafkaProducerService;
    @Autowired
    private SensorDataRepository sensorDataRepository;
    @Autowired
    private SensorAggregationService sensorAggregationService;
    @Autowired
    private MailService mailService;

    @GetMapping("/check-mqtt")
    public String checkMqtt(@Valid String key) {
        return mailService.getSensorStatusMqtt(key);
    }


    /**
     * Get the generated api-key for given sensor id.
     *
     * @param id
     * @return String
     */
    @GetMapping("/get-key")
    public String retKey(@Valid int id) {
        return AESUtil.getApiKey(String.valueOf(id));
    }

    /**
     * Get the decrypted sensor id from given api-key
     *
     * @param key
     * @return String
     */
    @GetMapping("/get-id")
    public String retId(String key) {
        return AESUtil.getSensorId(key);
    }

    /**
     * Get aggregated data for given time range as lastHours.
     *
     * @param key
     * @param lastHours
     * @return SensorDataSummary
     */
    @GetMapping("/agg")
    public ResponseEntity<SensorDataSummary> respondAggData(@RequestParam @NotEmpty String key, @RequestParam @NotNull @Valid int lastHours,
                                                            @RequestParam @NotNull @Valid int sampling, @RequestParam(value = "dateRange",
            required = false) List<Long> dateRange) {
        if (dateRange == null) {
            dateRange = Arrays.asList();
        }
        return ResponseEntity.ok().body(sensorAggregationService.aggregatedData2(key, lastHours, sampling, dateRange));
    }

    /**
     * Get sensor data.
     *
     * @param page
     * @return Page<SensorData>
     */
    @GetMapping("")
    public ResponseEntity<Page<SensorData>> respondDataAll(@PageableDefault(sort = {"id"}, direction = Sort.Direction.DESC, value = 10) @Valid Pageable page) {
        return ResponseEntity.ok().body(sensorDataRepository.findAll(page));
    }

    /**
     * Get sensor data by given api-keys.
     *
     * @param keys
     * @param page
     * @return Page<SensorData>
     */
    @PostMapping("/by-keys")
    public ResponseEntity<Page<SensorData>> respondData(@RequestBody @NotEmpty String[] keys, @RequestParam long lastHours, @PageableDefault(sort =
            {"id"}, direction = Sort.Direction.DESC, value = 10) @Valid Pageable page) {
        Date timestamp = new Date((new Date()).getTime() - TimeUnit.HOURS.toMillis(lastHours));
        return ResponseEntity.ok().body(sensorDataRepository.findByKeyInAndTimestampAfter(Arrays.asList(keys), timestamp, page));
    }

    /**
     * Send given sensor data to kafka topics.
     *
     * @param sensorParams
     * @return String
     */
//    @GetMapping("/in")
    //    public String acceptData(@RequestParam Map<String, String> sensorParams) {
    //        ObjectMapper objectMapper = new ObjectMapper();
    //        SensorData sensorData = objectMapper.convertValue(sensorParams, SensorData.class);
    //
    //        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    //        Set<ConstraintViolation<SensorData>> errors = validator.validate(sensorData);
    //
    //        if (!errors.isEmpty()) {
    //            throw new ConstraintViolationException(errors);
    //        }
    //
    //        kafkaProducerService.produce(sensorData.getKey(), sensorData);
    //        return "Accepted";
    //    }

    /**
     * Check the activity of sensor by key
     *
     * @param key
     * @param lastMinutes
     * @return
     */
    @GetMapping("/status")
    public String checkSensorStatus(@RequestParam String key, @RequestParam(defaultValue = "1") long lastMinutes) {
        return mailService.getSensorStatus(key, lastMinutes);
    }

    /**
     * Get single last data by key
     *
     * @param key
     * @return
     */
    @GetMapping("/last")
    public ResponseEntity<SensorData> respondDataLast(@RequestParam String key) {
        return ResponseEntity.ok().body(sensorDataRepository.findFirstByKeyOrderByIdDesc(key));
    }

}
