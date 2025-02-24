package com.example.frauddetectionconsumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.tensorflow.SavedModelBundle;

import org.tensorflow.ndarray.StdArrays;



import org.tensorflow.types.TFloat32;


@Service
public class FraudDetectionService {

    private static final Logger logger = LoggerFactory.getLogger(FraudDetectionService.class);
    private static final String MODEL_PATH = "C:/Users/Yasin/Videos/FraudDetectionBased/serving/models/fraud_model/1";
    private static final String INPUT_TENSOR_NAME = "serving_default_inputs:0";
    private static final String OUTPUT_TENSOR_NAME = "StatefulPartitionedCall:0";

    private static final double AMOUNT_MIN = 1.0;
    private static final double AMOUNT_MAX = 15000.0;
    private static final double TIME_MAX = 24.0;

    @Value("${fraud.threshold:0.5}")
    private double fraudThreshold;

    private SavedModelBundle model;

    @PostConstruct
    public void init() {
        try {
            model = SavedModelBundle.load(MODEL_PATH, "serve");
            logger.info("TensorFlow model loaded successfully");

        } catch (Exception e) {
            logger.error("Failed to initialize TensorFlow model", e);
            throw new RuntimeException("Model initialization failed", e);
        }
    }

    @PreDestroy
    public void cleanup() {
        if (model != null) {
            model.close();
            logger.info("TensorFlow model closed");
        }
    }

    public void processTransaction(String transactionData) {
        try {
            double amount = parseAmount(transactionData);
            double time = parseTime(transactionData);

            validateInput(amount, time);

            float fraudProbability = predictFraud(new float[]{(float) amount, (float) time});
            logTransactionResult(amount, time, fraudProbability);

        } catch (IllegalArgumentException e) {
            logger.warn("Invalid transaction data: {}", e.getMessage());
        } catch (Exception e) {
            logger.error("Error processing transaction: {}", transactionData, e);
        }
    }

    private void validateInput(double amount, double time) {
        if (amount < 0) throw new IllegalArgumentException("Negative amount");
        if (time < 0) throw new IllegalArgumentException("Negative time");
    }

    public float predictFraud(float[] rawInput) {
        double normalizedAmount = (rawInput[0] - AMOUNT_MIN) / (AMOUNT_MAX - AMOUNT_MIN);
        double normalizedTime = rawInput[1] / TIME_MAX;

        float[] normalizedFeatures = new float[]{(float) normalizedAmount, (float) normalizedTime};

        try (TFloat32 inputTensor = TFloat32.tensorOf(StdArrays.ndCopyOf(new float[][]{normalizedFeatures}));
             TFloat32 outputTensor = (TFloat32) model.session().runner()
                     .feed(INPUT_TENSOR_NAME, inputTensor)
                     .fetch(OUTPUT_TENSOR_NAME)
                     .run()
                     .get(0)) {
            return outputTensor.getFloat(0, 0);
        }
    }






    private void logTransactionResult(double amount, double time, double probability) {
        String status = probability > fraudThreshold ? "FRAUD" : "SAFE";
        String message = String.format("Transaction [Amount: %.2f, Time: %.2f] - Score: %.2f → %s",
                amount, time, probability, status);
        logger.info(message);
    }

    private double parseAmount(String data) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(data);
        return jsonNode.get("amount").asDouble();
    }

    private double parseTime(String data) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(data);
        return jsonNode.get("time").asDouble();
    }
}
