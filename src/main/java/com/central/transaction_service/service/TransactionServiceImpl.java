package com.central.transaction_service.service;

import com.central.transaction_service.kafka.KafkaEventProducer;
import com.central.transaction_service.model.Transaction;
import com.central.transaction_service.model.TransactionStatus;
import com.central.transaction_service.repository.TransactionRepository;
import com.central.transaction_service.repository.TransactionSpecifications;
import com.central.transaction_service.utils.ServiceUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.openapitools.model.StatusResponse;
import org.openapitools.model.StatusUpdateRequest;
import org.openapitools.model.TransactionRequest;
import org.openapitools.model.TransactionResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.central.transaction_service.utils.ServiceUtils.constructTransactionResponse;

@Slf4j
@Service
@Transactional
public class TransactionServiceImpl implements TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private KafkaEventProducer kafkaEventProducer;

    @Override
    public TransactionResponse createTransaction(TransactionRequest transactionRequest) {
        Transaction transaction = Transaction.builder()
                .senderId(transactionRequest.getSenderId())
                .receiverId(transactionRequest.getReceiverId())
                .amount(transactionRequest.getAmount())
                .description(transactionRequest.getDescription())
                .status(TransactionStatus.COMPLETED)
                .build();
        transaction = transactionRepository.save(transaction);
        log.info("Transaction created successfully with Transaction ID: {}", transaction.getTransaction_id());

        try {
            kafkaEventProducer.sendSenderTransactionEvent(transaction.getTransaction_id().toString(), transaction);
            log.info("Transaction event sent successfully with Transaction ID: {}", transaction.getTransaction_id());
        } catch (Exception e) {
            log.error("Failed to send transaction event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process transaction event");
        }

        return constructTransactionResponse(transaction);
    }

    @Override
    public Page<TransactionResponse> getUserTransactions(
            String userCode,
            String status,
            OffsetDateTime fromDate,
            OffsetDateTime toDate,
            Pageable pageable) {

        try {
            // Build specification based on filters
            Specification<Transaction> spec = (root, query, criteriaBuilder) -> criteriaBuilder.conjunction();

            // Add user code filter if provided
            if (StringUtils.isNotBlank(userCode)) {
                log.debug("Filtering transactions by user code: {}", userCode);
                spec = spec.and(TransactionSpecifications.hasUserCode(userCode));
            }

            // Add status filter if provided
            if (StringUtils.isNotBlank(status)) {
                try {
                    TransactionStatus transactionStatus = TransactionStatus.valueOf(status.toUpperCase());
                    log.debug("Filtering transactions by status: {}", transactionStatus);
                    spec = spec.and(TransactionSpecifications.hasStatus(transactionStatus));
                } catch (IllegalArgumentException ex) {
                    log.warn("Invalid status value provided: {}", status);
                    throw new IllegalArgumentException("Invalid status: " + status);
                }
            }

            // Add date range filters if provided
            LocalDateTime fromDateTime = fromDate != null ? fromDate.toLocalDateTime() : null;
            LocalDateTime toDateTime = toDate != null ? toDate.toLocalDateTime() : null;

            if (fromDateTime != null) {
                log.debug("Filtering transactions from date: {}", fromDateTime);
                spec = spec.and(TransactionSpecifications.createdAfter(fromDateTime));
            }

            if (toDateTime != null) {
                log.debug("Filtering transactions to date: {}", toDateTime);
                spec = spec.and(TransactionSpecifications.createdBefore(toDateTime));
            }

            // Get the page of results
            Page<Transaction> transactionsPage = transactionRepository.findAll(spec, pageable);

            // Log the query details
            log.debug("Executing query: {}", transactionsPage.toString());
            log.debug("Found {} items", transactionsPage.getTotalElements());

            // Map the results to TransactionResponse objects
            return transactionsPage.map(ServiceUtils::constructTransactionResponse);

        } catch (IllegalArgumentException ex) {
            log.error("Error listing transactions: {}", ex.getMessage(), ex);
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error listing transactions: {}", ex.getMessage(), ex);
            throw new RuntimeException("Internal Server Error");
        }
    }

    @Override
    public TransactionResponse getTransactionDetails(UUID transactionId) {
        log.info("Fetching transaction details for transaction ID: {}", transactionId);
        return transactionRepository.findById(transactionId)
                .map(ServiceUtils::constructTransactionResponse)
                .orElseThrow(() -> new EntityNotFoundException("Transaction not found with id: " + transactionId));
    }

    @Override
    public TransactionResponse updateTransactionStatus(UUID transactionId, StatusUpdateRequest statusUpdateRequest) {
        // Implementation to update transaction status
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new EntityNotFoundException("Transaction not found with id: " + transactionId));
        
        // Update status logic here
        // transaction.setStatus(...);
        
        transaction = transactionRepository.save(transaction);
        return constructTransactionResponse(transaction);
    }

    @Override
    public StatusResponse getTransactionStatus(UUID transactionId) {
        // Implementation to get transaction status
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new EntityNotFoundException("Transaction not found with id: " + transactionId));
        
        StatusResponse statusResponse = new StatusResponse();
        statusResponse.setStatus(StatusResponse.StatusEnum.valueOf(transaction.getStatus().name()));
        return statusResponse;
    }
}
