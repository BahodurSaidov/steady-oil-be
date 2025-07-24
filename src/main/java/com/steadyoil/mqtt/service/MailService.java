package com.steadyoil.mqtt.service;

import com.steadyoil.mqtt.domain.Sensor;
import com.steadyoil.mqtt.domain.SensorData;
import com.steadyoil.mqtt.dto.NotificationSummary;
import com.steadyoil.mqtt.repository.SensorDataRepository;
import com.steadyoil.mqtt.util.DomainConstants;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@CommonsLog
@Service
public class MailService {

    private final Pageable defaultPage = new Pageable() {
        @Override
        public int getPageNumber() {
            return 0;
        }

        @Override
        public int getPageSize() {
            return 10;
        }

        @Override
        public long getOffset() {
            return 0;
        }

        @Override
        public Sort getSort() {
            return null;
        }

        @Override
        public Pageable next() {
            return null;
        }

        @Override
        public Pageable previousOrFirst() {
            return null;
        }

        @Override
        public Pageable first() {
            return null;
        }

        @Override
        public Pageable withPage(int pageNumber) {
            return null;
        }

        @Override
        public boolean hasPrevious() {
            return false;
        }
    };

    @Autowired
    private EmqxService emqxService;
    @Autowired
    private SensorDataRepository sensorDataRepository;
    @Autowired
    private SensorService sensorService;
    @Autowired
    private NotificationService notificationService;
    @Value("${com.steadyoil.mail.key}")
    private String mailKey;
    @Value("${com.steadyoil.mail.from}")
    private String mailFrom;
    @Value("${com.steadyoil.mail.thresholdMinCount}")
    private int minCount;
    @Value("${com.steadyoil.mail.thresholdLastMinutes}")
    private int lastMinutes;

    @Async
    void checkStatusAndSendMail(Long sensorId, List<String> emails, String clientLabel) {
        Sensor sensor = sensorService.getSensorById(sensorId);
        if (emails != null && sensor != null && sensor.getStatus().equals(DomainConstants.SENSOR_STATUS_ON)) {
            final String key = sensor.getKey();
            //            final String status = getSensorStatus(key, 1);
            final String status = getSensorStatusMqtt(key);
            final SensorData sensorDataLast = sensorDataRepository.findFirstByKeyOrderByIdDesc(key);
            if (status.equals(DomainConstants.SENSOR_ACTIVITY_ACTIVE) && (sensor.getActivity() == null || sensor.getActivity().equals(DomainConstants.SENSOR_ACTIVITY_INACTIVE))) {
                sensor.setActivity(DomainConstants.SENSOR_ACTIVITY_ACTIVE);
                sensorService.updateSave(sensor);
            } else if (status.equals(DomainConstants.SENSOR_ACTIVITY_INACTIVE) && sensor.getActivity().equals(DomainConstants.SENSOR_ACTIVITY_ACTIVE)) {
                sensor.setActivity(DomainConstants.SENSOR_ACTIVITY_INACTIVE);
                sensorService.updateSave(sensor);
            }

            if (status.equals(DomainConstants.SENSOR_ACTIVITY_INACTIVE)) {
                DateFormat formatter = new SimpleDateFormat("dd MMM yyyy HH:mm:ss z");
                formatter.setTimeZone(TimeZone.getTimeZone("EST"));

                for (String email : emails) {
                    log.info("Mail sending to " + email.toString());
                    String title = "WARNING: " + sensor.getLabel() + " is Offline!";
                    // @formatter:off
                    String description = "The following sensor is Offline:\n" +
                            "\nClient: "+clientLabel+
                            "\nSensor: "+ sensor.getLabel() +
                            "\nKey: " + key +
                            "\nMAC: " + sensor.getMac() +
                            "\nLast Online: " + formatter.format(sensorDataLast.getTimestamp())+
                            "\n\nPlease contact administrator for further details.";

                    sendTextEmail(email, title, description);
                    // @formatter:on
                }
            }
        }
    }

    @Async
    public void sendTextEmail(String clientEmail, String title, String description) {
        Email from = new Email(mailFrom);
        Email to = new Email(clientEmail);
        Content content = new Content("text/plain", description);
        Mail mail = new Mail(from, title, to, content);

        SendGrid sg = new SendGrid(mailKey);
        Request request = new Request();
        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sg.api(request);
            log.info("Mail sent to " + to + " having title " + title);
        } catch (IOException ex) {
            log.error("Mail error: " + ex.getMessage());
        }
    }

    public String getSensorStatus(String key, long lastMinutes) {
        Date timestamp = new Date((new Date()).getTime() - TimeUnit.MINUTES.toMillis(lastMinutes));
        long count = sensorDataRepository.countByKeyAndTimestampAfter(key, timestamp);
        if (count == 0) {
            return DomainConstants.SENSOR_ACTIVITY_INACTIVE;
        } else {
            return DomainConstants.SENSOR_ACTIVITY_ACTIVE;
        }
    }

    public String getSensorStatusMqtt(String key) {
        Map<String, Object> clientDetails = emqxService.getClientDetails(key);
        if (clientDetails == null || clientDetails.get("connected").equals(false)) {
            return DomainConstants.SENSOR_ACTIVITY_INACTIVE;
        } else if (clientDetails.get("connected").equals(true)) {
            return DomainConstants.SENSOR_ACTIVITY_ACTIVE;
        }
        return DomainConstants.SENSOR_ACTIVITY_INACTIVE;
    }

    void checkThresholdAndSendMail(Long sensorId, List<String> emails, String clientLabel) {
        Sensor sensor = sensorService.getSensorById(sensorId);
        if (emails != null && sensor != null) {
            log.info("checkThresholdAndSendMail start");
            final String key = sensor.getKey();
            Sensor.Thresholds thresholds = sensor.getThresholds();

            Page<NotificationSummary> notificationSummaryPage = notificationService.aggregatedData(Arrays.asList(key), lastMinutes, defaultPage,
                    true);

            notificationSummaryPage.getContent().forEach(n -> {

                String thresholdProperty = n.getThresholdProperty();
                String thresholdPropertyStr =
                        Arrays.stream(thresholdProperty.split("_")).map((s) -> s.substring(0, 1).toUpperCase() + s.substring(1)).collect(Collectors.joining(""));

                Sensor.Threshold threshold;
                try {
                    java.lang.reflect.Method sensorThresholdGetter = thresholds.getClass().getMethod("get" + thresholdPropertyStr);
                    threshold = (Sensor.Threshold) sensorThresholdGetter.invoke(thresholds);
                } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                    log.error("checkThresholdAndSendMail error:" + e.getMessage());
                    throw new RuntimeException(e);
                }

                if (threshold.isMail()) {
                    if (n.getCount() >= minCount) {
                        // send mail
                        DateFormat formatter = new SimpleDateFormat("dd MMM yyyy HH:mm:ss z");
                        formatter.setTimeZone(TimeZone.getTimeZone("EST"));

                        for (String email : emails) {
                            log.info("Mail sending to " + email.toString());
                            String title =
                                    "WARNING: " + sensor.getLabel() + " has Threshold Alarm for " + n.getThresholdBoundary().toUpperCase() + " " + thresholdPropertyStr + " " + n.getCount() + " times !";
                            // @formatter:off
                            String description = "Alarm details:\n";
                            description +=
                                    "\nData Value " + n.getDataValue() + " is " + n.getThresholdBoundary().toUpperCase() + "-er" + " " +
                                            "than the Threshold Value " + n.getThresholdValue() + " for " + thresholdPropertyStr + " occurred "+n.getCount() + " times !";

                            description += "\n"+
                                    "\nClient: "+clientLabel+
                                    "\nSensor: "+ sensor.getLabel() +
                                    "\nKey: " + key +
                                    "\nMAC: " + sensor.getMac() +
                                    "\nTimestamp: " + formatter.format(n.getTimestamp())+
                                    "\n\nPlease contact administrator for further details.";

                            sendTextEmail(email, title, description);
                            // @formatter:on
                        }
                    }
                }
            });

            log.info("checkThresholdAndSendMail finish");
        }
    }
}
