package com.steadyoil.mqtt.config;

import com.steadyoil.mqtt.service.MqttInboundService;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.web.client.RestTemplate;

@Configuration
public class MqttConfig {

    @Value("${mqtt.broker.url}")
    private String brokerUrl;

    @Value("${mqtt.broker.client_id}")
    private String clientId;

    @Value("${mqtt.broker.username}")
    private String username;

    @Value("${mqtt.broker.password}")
    private String password;

    @Value("${mqtt.topic.subscribe}")
    private String mqttTopicSubscribe;

    @Value("${mqtt.qos.subscribe}")
    private int mqttQosSubscribe;

    private MqttInboundService mqttInboundService;

    @Autowired
    public MqttConfig(MqttInboundService mqttInboundService) {
        this.mqttInboundService = mqttInboundService;
    }

    @Bean
    public MessageChannel mqttInputChannel() {
        return new DirectChannel();
    }

    /**
     * Configure the MQTT connection options.
     */
    @Bean
    public MqttConnectOptions mqttConnectOptions() {
        MqttConnectOptions options = new MqttConnectOptions();
        options.setUserName(username);
        options.setPassword(password.toCharArray());
        options.setServerURIs(new String[]{brokerUrl});
        options.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);
        options.setKeepAliveInterval(15);
        options.setConnectionTimeout(30);
        options.setAutomaticReconnect(true);
        options.setCleanSession(false);

        return options;
    }


    /**
     * MQTT client factory that uses the configured MqttConnectOptions.
     */
    @Bean
    public MqttPahoClientFactory mqttClientFactory(MqttConnectOptions mqttConnectOptions) {
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        factory.setConnectionOptions(mqttConnectOptions);
        return factory;
    }

    @Bean
    public MessageProducer inbound(MqttPahoClientFactory mqttClientFactory) {
        MqttPahoMessageDrivenChannelAdapter adapter = new MqttPahoMessageDrivenChannelAdapter(clientId, mqttClientFactory, mqttTopicSubscribe);
        adapter.setCompletionTimeout(5000);
        adapter.setConverter(new DefaultPahoMessageConverter());
        adapter.setQos(mqttQosSubscribe);
        adapter.setOutputChannel(mqttInputChannel());
        return adapter;
    }

    /**
     * The channel to which inbound messages will be sent.
     */
    @Bean
    public MessageChannel mqttInboundChannel() {
        return new DirectChannel();
    }

    /**
     * Outbound channel where to send messages destined for MQTT.
     */
    @Bean
    public MessageChannel mqttOutboundChannel() {
        return new DirectChannel();
    }

    /**
     * The outbound adapter (MessageHandler) that actually publishes messages to the MQTT broker.
     * - `@ServiceActivator` means that any message sent to `mqttOutboundChannel`
     * will be handled by this bean.
     * - We set a `defaultTopic` to "sensor/cmd". You can also pass a topic dynamically
     * via message headers if needed.
     */
    @Bean
    @ServiceActivator(inputChannel = "mqttOutboundChannel")
    public MessageHandler mqttOutbound(@Autowired MqttPahoClientFactory mqttClientFactory) {
        MqttPahoMessageHandler messageHandler = new MqttPahoMessageHandler(clientId, mqttClientFactory);
        messageHandler.setAsync(true);

        return messageHandler;
    }

    @Bean
    @ServiceActivator(inputChannel = "mqttInputChannel")
    public MessageHandler handler() {
        return message -> {
            mqttInboundService.messageArrived(message.getPayload().toString(), message.getHeaders());
        };
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

}