package com.example.frauddetectionproducer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;



@RestController
public class FraudDetectionProducerController {

    @Autowired
    private KafkaProducerService kafkaProducerService;

    @GetMapping("/send-transaction")
    public String sendTransaction(@RequestParam double amount, @RequestParam int time) {
        String transactionData = String.format("{\"amount\": %.2f, \"time\": %d}", amount, time);
        kafkaProducerService.sendTransaction(transactionData);
        return "Transaction was processed successfully";
    }

}