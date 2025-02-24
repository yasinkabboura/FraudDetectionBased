package com.example.frauddetectionconsumer;

import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@EnableKafka
public class KafkaConsumerService {

    private final FraudDetectionService fraudDetectionService;

    public KafkaConsumerService(FraudDetectionService fraudDetectionService) {
        this.fraudDetectionService = fraudDetectionService;
    }

    @KafkaListener(topics = "fraud-detection-topic", groupId = "fraud-consumer-group")
    public void consume(String transactionData) {
        fraudDetectionService.processTransaction(transactionData);
    }
}