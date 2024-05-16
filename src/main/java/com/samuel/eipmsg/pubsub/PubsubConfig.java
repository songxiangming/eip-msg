package com.samuel.eipmsg.pubsub;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.samuel.eipmsg.Payment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.gcp.pubsub.core.PubSubTemplate;
import org.springframework.cloud.gcp.pubsub.integration.AckMode;
import org.springframework.cloud.gcp.pubsub.integration.inbound.PubSubInboundChannelAdapter;
import org.springframework.cloud.gcp.pubsub.integration.outbound.PubSubMessageHandler;
import org.springframework.cloud.gcp.pubsub.support.*;
import org.springframework.cloud.gcp.pubsub.support.converter.JacksonPubSubMessageConverter;
import org.springframework.cloud.gcp.pubsub.support.converter.PubSubMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;


@Configuration
@IntegrationComponentScan(basePackages = "com.samuel.eipmsg.pubsub")
public class PubsubConfig {
    private static final Logger logger = LogManager.getLogger(PubsubConfig.class);
    public static final String TOPIC_NAME = "payments";
    public static final String SUBSCRIPTION_NAME = "payments_sub_code";

    //<editor-fold desc="pub">
    @MessagingGateway(defaultRequestChannel = "pubsubOutputChannel")
    public interface PubsubGateway {
        void sendMessage(Payment payment);
    }

    @Bean
    public MessageChannel pubsubOutputChannel() {
        return new DirectChannel();
    }


    @Bean
    @ServiceActivator(inputChannel = "pubsubOutputChannel")
    public MessageHandler pubsubMessageHandler(PubSubTemplate pubSubTemplate) {
        PubSubMessageHandler publishMsgHandler = new PubSubMessageHandler(pubSubTemplate, TOPIC_NAME);
        publishMsgHandler.setSync(true);
        pubSubTemplate.setMessageConverter(pubSubMessageConverter());
        return publishMsgHandler;
    }


    /*@Bean
    public PubSubMessageConverter pubSubMessageConverter() {
        return new AvroPubSubMessageConverter();
    }*/

    // Pub/Sub Configuration
    @Bean
    public PubSubMessageConverter pubSubMessageConverter() {
        return new JacksonPubSubMessageConverter(new ObjectMapper());
    }

    //</editor-fold>


    //<editor-fold desc="sub">

    @Bean
    public PubSubInboundChannelAdapter pubsubMessageInboundAdapter(
            @Qualifier("pubsubInputChannel") MessageChannel inputChannel,
            PubSubTemplate pubSubTemplate) {
        pubSubTemplate.setMessageConverter(pubSubMessageConverter());
        PubSubInboundChannelAdapter adapter =
                new PubSubInboundChannelAdapter(pubSubTemplate, SUBSCRIPTION_NAME);
        adapter.setPayloadType(Payment.class);
        adapter.setOutputChannel(inputChannel);
        adapter.setAckMode(AckMode.MANUAL);
        return adapter;
    }

    @Bean
    public MessageChannel pubsubInputChannel() {
        return new DirectChannel();
    }



    @Bean
    @ServiceActivator(inputChannel = "pubsubInputChannel")
    public MessageHandler pubsubMessageReceiver() {
        return message -> {
            logger.info("Message arrived! Payload: " +  message.toString());
            BasicAcknowledgeablePubsubMessage originalMessage =
                    message.getHeaders().get(GcpPubSubHeaders.ORIGINAL_MESSAGE, BasicAcknowledgeablePubsubMessage.class);
            originalMessage.ack();
        };
    }

    //</editor-fold>

}
