package com.steadyoil.mqtt.controller;

import com.steadyoil.mqtt.domain.Notification;
import com.steadyoil.mqtt.dto.NotificationSummary;
import com.steadyoil.mqtt.service.NotificationService;
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
import java.util.List;

@CrossOrigin(origins = "${com.steadyoil.cors.url}")
@RequestMapping("/api/v1/notifications")
@RestController
public class NotificationApiController {

    @Autowired
    private NotificationService notificationService;

    /**
     * Get notifications by given api-keys and lastHours.
     *
     * @param keys
     * @param page
     * @return Page<SensorData>
     */
    @PostMapping("/by-keys")
    public ResponseEntity<Page<Notification>> respondData(@RequestBody @NotEmpty String[] keys, @RequestParam @NotNull @Valid int lastHours,
                                                          @PageableDefault(sort = {"id"}, direction = Sort.Direction.DESC, value = 10) @Valid Pageable page) {
        return ResponseEntity.ok().body(notificationService.getNotificationsByKeysAndLastHours(Arrays.asList(keys), lastHours, page));
    }

    @GetMapping("/{id}/is_read")
    public ResponseEntity<Notification> updateSensor(@PathVariable long id, @RequestParam(value = "is_read") @Valid boolean isRead) {
        return ResponseEntity.ok().body(notificationService.updateNotificationIsRead(id, isRead));
    }

    @PostMapping("/by-keys-agg")
    public ResponseEntity<Page<NotificationSummary>> respondAggData(@RequestBody @NotEmpty String[] keys,
                                                                    @RequestParam @NotNull @Valid int lastHours, @PageableDefault(sort = {"id"},
            direction = Sort.Direction.DESC, value = 10) @Valid Pageable page,
                                                                    @RequestParam(value = "dateRange", required = false) List<Long> dateRange) {
        if (dateRange == null) {
            dateRange = Arrays.asList();
        }
        return ResponseEntity.ok().body(notificationService.aggregatedData(Arrays.asList(keys), lastHours, page, false, dateRange));
    }

    @PostMapping("/by-keys-agg-search")
    public ResponseEntity<Page<NotificationSummary>> respondSearchAggData(@RequestParam String parameter, @RequestParam String condition,
                                                                          @RequestParam Double value, @RequestBody @NotEmpty String[] keys,
                                                                          @RequestParam @NotNull @Valid int lastHours, @PageableDefault(sort = {"id"
    }, direction = Sort.Direction.DESC, value = 10) @Valid Pageable page, @RequestParam(value = "dateRange", required = false) List<Long> dateRange) {
        if (dateRange == null) {
            dateRange = Arrays.asList();
        }
        return ResponseEntity.ok().body(notificationService.aggregatedData(Arrays.asList(keys), lastHours, page, false, dateRange, parameter,
                condition, value));
    }

}
