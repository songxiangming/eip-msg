package com.samuel.eipmsg.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.samuel.eipmsg.Payment;
import com.samuel.eipmsg.pubsub.PubsubConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.gcp.pubsub.core.PubSubTemplate;
import org.springframework.cloud.gcp.pubsub.integration.AckMode;
import org.springframework.cloud.gcp.pubsub.integration.inbound.PubSubInboundChannelAdapter;
import org.springframework.cloud.gcp.pubsub.integration.outbound.PubSubMessageHandler;
import org.springframework.cloud.gcp.pubsub.support.BasicAcknowledgeablePubsubMessage;
import org.springframework.cloud.gcp.pubsub.support.GcpPubSubHeaders;
import org.springframework.cloud.gcp.pubsub.support.converter.JacksonPubSubMessageConverter;
import org.springframework.cloud.gcp.pubsub.support.converter.PubSubMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.annotation.InboundChannelAdapter;
import org.springframework.integration.annotation.IntegrationComponentScan;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.kafka.inbound.KafkaMessageDrivenChannelAdapter;
import org.springframework.integration.kafka.outbound.KafkaProducerMessageHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.TopicPartitionOffset;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

@Configuration
@IntegrationComponentScan(basePackages = "com.samuel.eipmsg.kafka")
public class KafkaConfig {

    private static final Logger logger = LogManager.getLogger(KafkaConfig.class);
    public static final String TOPIC_NAME = "test_topic_0";

    //<editor-fold desc="pub">
    @MessagingGateway(defaultRequestChannel = "kafkaOutputChannel")
    public interface KafkaGateway {
        void sendMessage(Payment payment);
    }

    @Bean
    public MessageChannel kafkaOutputChannel() {
        return new DirectChannel();
    }

    @Bean
    @ServiceActivator(inputChannel = "kafkaOutputChannel")
    public MessageHandler kafkaMessageHandler(KafkaTemplate<String, Payment> kafkaTemplate) {
        KafkaProducerMessageHandler<String, Payment> handler =
                new KafkaProducerMessageHandler<>(kafkaTemplate);
        handler.setTopicExpression(new LiteralExpression(TOPIC_NAME));
        handler.setMessageKeyExpression(new SpelExpressionParser().parseExpression("payload.id"));
        return handler;
    }
    //</editor-fold>

    //<editor-fold desc="sub">

    @Bean
    public KafkaMessageListenerContainer<String, String> container(
            ConsumerFactory<String, String> kafkaConsumerFactory) {
        KafkaMessageListenerContainer<String, String> listenerContainer = new KafkaMessageListenerContainer<>(kafkaConsumerFactory,
                new ContainerProperties(TOPIC_NAME));
        return listenerContainer;
    }

    @Bean
    public KafkaMessageDrivenChannelAdapter<String, String> kafkaMessageInboundAdapter(
            @Qualifier("kafkaInputChannel") MessageChannel kafkaInputChannel,
            KafkaMessageListenerContainer<String, String> container) {

        KafkaMessageDrivenChannelAdapter<String, String> kafkaMessageDrivenChannelAdapter =
                new KafkaMessageDrivenChannelAdapter<>(container);
        kafkaMessageDrivenChannelAdapter.setOutputChannel(kafkaInputChannel);
        return kafkaMessageDrivenChannelAdapter;
    }

    @Bean
    public MessageChannel kafkaInputChannel() {
        return new DirectChannel();
    }

    @Bean
    @ServiceActivator(inputChannel = "kafkaInputChannel")
    public MessageHandler kafkaMessageReceiver() {
        return message -> {
            logger.info("Message arrived! Payload: {}", message.toString());

        };
    }

    @KafkaListener(topics = TOPIC_NAME, groupId = "payment-group")
    public void listen(Payment payment, Acknowledgment acknowledgment) {
        logger.info("Received payment: {}", payment);

        // Manually commit the offset
        acknowledgment.acknowledge();
    }

    //</editor-fold>

}
