package com.samuel.eipmsg;


import com.samuel.eipmsg.kafka.KafkaConfig;
import com.samuel.eipmsg.pubsub.PubsubConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/payments")
public class PaymentsController {

    private static final Logger logger = LogManager.getLogger(PaymentsController.class);

    @Autowired
    private PubsubConfig.PubsubGateway pubsubGateway;

    @Autowired
    private KafkaConfig.KafkaGateway kafkaGateway;

    @PostMapping
    public ResponseEntity<Payment> createPayment(@RequestBody Payment payment) {

        if ("USD".equals(payment.getCurrency())) {
            logger.debug("currency is USD, send msg via pub/sub");
            pubsubGateway.sendMessage(payment);
        } else {
            logger.debug("currency is Not USD, send msg via Kafka");
            kafkaGateway.sendMessage(payment);
        }
        logger.debug("send msg complete");

        // Return the created user and HTTP status code 201 (CREATED)
        return new ResponseEntity<>(payment, HttpStatus.CREATED);
    }
}