package com.samuel.eipmsg.pubsub;


import com.google.protobuf.ByteString;
import com.google.pubsub.v1.PubsubMessage;
import com.samuel.eipmsg.Payment;
import org.apache.avro.io.*;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificDatumWriter;
import org.springframework.cloud.gcp.pubsub.support.converter.PubSubMessageConverter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

public class AvroPubSubMessageConverter implements PubSubMessageConverter {

    @Override
    public PubsubMessage toPubSubMessage(Object payload, Map<String, String> headers) {
        if (!(payload instanceof Payment)) {
            throw new IllegalArgumentException("Payload must be of type Payment.");
        }

        Payment payment = (Payment) payload;
        DatumWriter<Payment> writer = new SpecificDatumWriter<>(Payment.class);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        BinaryEncoder encoder = EncoderFactory.get().binaryEncoder(outputStream, null);

        try {
            writer.write(payment, encoder);
            encoder.flush();
        } catch (IOException e) {
            throw new RuntimeException("Failed to serialize Payment to Avro", e);
        }

        PubsubMessage.Builder messageBuilder = PubsubMessage.newBuilder()
                .setData(ByteString.copyFrom(outputStream.toByteArray()));

        headers.forEach(messageBuilder::putAttributes);

        return messageBuilder.build();
    }

    @Override
    public <T> T fromPubSubMessage(PubsubMessage message, Class<T> payloadType) {
        if (!payloadType.isAssignableFrom(Payment.class)) {
            throw new IllegalArgumentException("Payload type must be Payment.");
        }

        byte[] data = message.getData().toByteArray();
        DatumReader<T> reader = new SpecificDatumReader<>(payloadType);
        BinaryDecoder decoder = DecoderFactory.get().binaryDecoder(data, null);

        try {
            return reader.read(null, decoder);
        } catch (IOException e) {
            throw new RuntimeException("Failed to deserialize Payment from Avro", e);
        }
    }
}