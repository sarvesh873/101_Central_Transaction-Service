package com.central.transaction_service.kafka;

import com.central.transaction_service.model.Transaction;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.protobuf.Timestamp;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import transaction.events.TransactionEvent;

import java.time.ZoneOffset;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
public class KafkaEventProducer {
    private static final String TOPIC = "txn-initiated";

    private final KafkaTemplate<String,  byte[]> kafkaTemplate;

    @Autowired
    public KafkaEventProducer(KafkaTemplate<String,  byte[]> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendTransactionEvent(String key, Transaction transaction) {
        log.info("📤 Sending to Kafka → Topic: {}, Key: {}, Message: {}", TOPIC, key, transaction);

        TransactionEvent transactionEvent = TransactionEvent.newBuilder()
                .setTransactionId(transaction.getTransaction_id().toString())
                .setSenderId(transaction.getSenderId())
                .setReceiverId(transaction.getReceiverId())
                .setAmount(transaction.getAmount())
                .setStatus(transaction.getStatus())
                .setCreatedAt(Timestamp.newBuilder()
                        .setSeconds(transaction.getInitiatedAt().toEpochSecond(ZoneOffset.UTC))
                        .setNanos(0)
                        .build())
                .setUpdatedAt(Timestamp.newBuilder()
                        .setSeconds(transaction.getUpdatedAt().toEpochSecond(ZoneOffset.UTC))
                        .setNanos(0)
                        .build())
                .build();
        CompletableFuture<SendResult<String,  byte[]>> future = kafkaTemplate.send(TOPIC, key, transactionEvent.toByteArray());

        future.thenAccept(result -> {
            RecordMetadata metadata = result.getRecordMetadata();
            log.info("Kafka message sent successfully! Topic: {}, Partition: {}, Offset: {}", metadata.topic(), metadata.partition(), metadata.offset());
        }).exceptionally(ex -> {
            log.error("Failed to send Kafka message: {}", ex.getMessage(), ex);
            ex.printStackTrace();
            return null;
        });
    }

}
