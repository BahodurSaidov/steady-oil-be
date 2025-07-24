package com.steadyoil.mqtt.controller;

import com.steadyoil.mqtt.domain.Sensor;
import com.steadyoil.mqtt.service.ClientService;
import com.steadyoil.mqtt.service.SensorService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "${com.steadyoil.cors.url}")
@RequestMapping("/api/v1/sensors")
@RestController
public class SensorApiController {
    @Autowired
    private SensorService sensorService;

    @Autowired
    private ClientService clientService;

    /**
     * Get all sensors accessible by client.
     *
     * @param id
     * @return Page<Sensor>
     */
    @GetMapping("/client/{id}")
    public ResponseEntity<Page<Sensor>> getAllSensor(@PathVariable long id, @PageableDefault(sort = {"id"}, direction = Sort.Direction.DESC, value
            = 10) @Valid Pageable page) {
        List<Long> idList = clientService.getClientById(id).getSensors();
        return ResponseEntity.ok().body(sensorService.getAllSensorByIdIn(idList, page));
    }

    /**
     * Get all sensors given by ids.
     *
     * @param idList
     * @return Page<Sensor>
     */
    @PostMapping("/selected")
    public ResponseEntity<Page<Sensor>> getAllSensor(@RequestBody @NotEmpty @Valid List<Long> idList, @PageableDefault(sort = {"id"}, direction =
            Sort.Direction.DESC, value = 10) @Valid Pageable page) {
        return ResponseEntity.ok().body(sensorService.getAllSensorByIdIn(idList, page));
    }

    /**
     * Get all sensors.
     *
     * @return Page<Sensor>
     */
    @GetMapping("")
    public ResponseEntity<Page<Sensor>> getAllSensor(@PageableDefault(sort = {"id"}, direction = Sort.Direction.DESC, value = 10) @Valid Pageable page) {
        return ResponseEntity.ok().body(sensorService.getAllSensor(page));
    }

    /**
     * Get sensor by id.
     *
     * @param id
     * @return Sensor
     */
    @GetMapping("/{id}")
    public ResponseEntity<Sensor> getSensorById(@PathVariable long id) {
        return ResponseEntity.ok().body(sensorService.getSensorById(id));
    }

    /**
     * Save sensor object.
     *
     * @param sensor
     * @return Sensor
     */
    @PostMapping("")
    public ResponseEntity<Sensor> createSensor(@RequestBody @Valid Sensor sensor) {
        return ResponseEntity.ok().body(this.sensorService.createSensor(sensor));
    }

    /**
     * Update sensor details by id
     *
     * @param id
     * @param sensor
     * @return
     */
    @PutMapping("/{id}")
    public ResponseEntity<Sensor> updateSensor(@PathVariable long id, @RequestBody @Valid Sensor sensor) {
        sensor.setId(id);
        return ResponseEntity.ok().body(this.sensorService.updateSensor(sensor));
    }

    /**
     * Delete sensor by id
     *
     * @param id
     * @return
     */
    @DeleteMapping("/{id}")
    public HttpStatus deleteSensor(@PathVariable long id) {
        this.sensorService.deleteSensor(id);
        return HttpStatus.OK;
    }

    /**
     * Update sensor thresholds by id
     *
     * @param id
     * @param thresholds
     * @return
     */
    @PutMapping("/{id}/thresholds")
    public ResponseEntity<Sensor> updateSensor(@PathVariable long id, @RequestBody @Valid Sensor.Thresholds thresholds) {
        return ResponseEntity.ok().body(sensorService.updateSensorThresholds(id, thresholds));
    }

    /**
     * Get clients who have access to current sensor id
     * @param id
     * @return
     */
    @GetMapping("/{id}/clients")
    public ResponseEntity<List<String>> getSensorClients(@PathVariable long id) {
        return ResponseEntity.ok().body(sensorService.getSensorClients(id));
    }
}
