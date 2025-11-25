package com.central.transaction_service.service;

import com.central.transaction_service.kafka.KafkaEventProducer;
import com.central.transaction_service.model.Transaction;
import com.central.transaction_service.repository.TransactionRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.openapitools.model.StatusResponse;
import org.openapitools.model.StatusUpdateRequest;
import org.openapitools.model.TransactionRequest;
import org.openapitools.model.TransactionResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

import static com.central.transaction_service.utils.ServiceUtils.constructTransactionResponse;

@Slf4j
@Service
@Transactional
public class TransactionServiceImpl implements  TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private KafkaEventProducer kafkaEventProducer;

    @Override
    public ResponseEntity<TransactionResponse> createTransaction(TransactionRequest transactionRequest) {
        Transaction transaction = Transaction.builder()
                .senderId(transactionRequest.getSenderId())
                .receiverId(transactionRequest.getReceiverId())
                .amount(transactionRequest.getAmount())
                .description(transactionRequest.getDescription())
                .status("SUCCESS")
                .build();
        transactionRepository.save(transaction);
        log.info("Transaction created successfully with Transaction ID : {}", transaction.getTransaction_id());

        try{
            kafkaEventProducer.sendReceiverTransactionEvent(transaction.getTransaction_id().toString(), transaction);
            log.info("Transaction event sent successfully with Transaction ID : {}", transaction.getTransaction_id());
        }
        catch (Exception e){
            log.error("Failed to create transaction: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(constructTransactionResponse(transaction));
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(constructTransactionResponse(transaction));
    }

    @Override
    public ResponseEntity<List<TransactionResponse>> getUserTransactions(String userCode) {
        return null;
    }

    @Override
    public ResponseEntity<TransactionResponse> getTransactionDetails(UUID transactionId) {
        log.info("Fetching transaction details for transaction ID: {}", transactionId);
        return transactionRepository.findById(transactionId)
                .map(transaction -> ResponseEntity.ok(constructTransactionResponse(transaction)))
                .orElse(ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<TransactionResponse> updateTransactionStatus(UUID transactionId, StatusUpdateRequest statusUpdateRequest) {
        return null;
    }

    @Override
    public ResponseEntity<StatusResponse> getTransactionStatus(UUID transactionId) {
        return null;
    }
}
