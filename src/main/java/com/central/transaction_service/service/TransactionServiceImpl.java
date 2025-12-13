package com.central.transaction_service.service;

import com.central.transaction_service.exception.GrpcServiceException;
import com.central.transaction_service.exception.InvalidTransactionStatusException;
import com.central.transaction_service.exception.TransactionNotFoundException;
import com.central.transaction_service.exception.TransactionProcessingException;
import com.central.transaction_service.grpc.WalletServiceGrpcClient;
import com.central.transaction_service.kafka.KafkaEventProducer;
import com.central.transaction_service.model.Transaction;
import com.central.transaction_service.model.TransactionStatus;
import com.central.transaction_service.repository.TransactionRepository;
import com.central.transaction_service.repository.TransactionSpecifications;
import com.central.transaction_service.dto.*;
import com.central.wallet.WalletResponseGRPC;
import org.openapitools.model.OverallStatusEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.apache.commons.lang3.StringUtils;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;



@Slf4j
@Service
@Transactional
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;

    private final KafkaEventProducer kafkaEventProducer;

    private final WalletServiceGrpcClient walletServiceGrpcClient;

    public TransactionServiceImpl(TransactionRepository transactionRepository, KafkaEventProducer kafkaEventProducer, WalletServiceGrpcClient walletServiceGrpcClient) {
        this.transactionRepository = transactionRepository;
        this.kafkaEventProducer = kafkaEventProducer;
        this.walletServiceGrpcClient = walletServiceGrpcClient;
    }

    @Override
    public TransactionResponseDto createTransaction(TransactionRequestDto transactionRequest) {
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
            log.info("Fetching wallet details for receiver ID: {}", transactionRequest.getReceiverId());
            WalletResponseGRPC walletResponseGRPC = walletServiceGrpcClient.getUserWallet(transactionRequest.getReceiverId());
            log.info("Successfully fetched wallet details for receiver ID: {}", walletResponseGRPC.getUserCode());
            log.info("Successfully fetched wallet details: {}", walletResponseGRPC);
        }
        catch (GrpcServiceException e){
            log.error("Error while fetching GRPC wallet details for receiver ID: {}. Error: {}",
                    transactionRequest.getReceiverId(), e.getMessage(), e);
            throw e;
        }
        catch (Exception e) {
            log.error("Error while fetching wallet details for receiver ID: {}. Error: {}", 
                transactionRequest.getReceiverId(), e.getMessage(), e);
            throw new TransactionProcessingException("Failed to process wallet details for receiver: " +
                transactionRequest.getReceiverId(), e);
        }

//        try {
//            kafkaEventProducer.sendSenderTransactionEvent(transaction.getTransaction_id().toString(), transaction);
//            log.info("Transaction event sent successfully with Transaction ID: {}", transaction.getTransaction_id());
//        } catch (Exception e) {
//            log.error("Failed to send transaction event: {}", e.getMessage(), e);
//            throw new TransactionProcessingException("Failed to process transaction event", e);
//        }

        return new TransactionResponseAdapter(transaction);
    }

    @Override
    public Page<TransactionResponseDto> getUserTransactions(
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
                    throw new InvalidTransactionStatusException("Invalid status value: '" + status + "'. Please provide a valid status.");
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
            return transactionsPage.map(TransactionResponseAdapter::new);

        } catch (IllegalArgumentException ex) {
            log.error("Error listing transactions: {}", ex.getMessage(), ex);
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error listing transactions: {}", ex.getMessage(), ex);
            throw new RuntimeException("Internal Server Error");
        }
    }

    @Override
    public TransactionResponseDto getTransactionDetails(UUID transactionId) {
        log.info("Fetching transaction details for transaction ID: {}", transactionId);
        return transactionRepository.findById(transactionId)
                .map(TransactionResponseAdapter::new)
                .orElseThrow(() -> new TransactionNotFoundException("Transaction not found with id: " + transactionId));
    }

    @Override
    public TransactionResponseDto updateTransactionStatus(UUID transactionId, StatusUpdateRequestDto statusUpdateRequest) {
        // Implementation to update transaction status
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException("Transaction not found with id: " + transactionId));
        
        transaction.setStatus(TransactionStatus.valueOf(statusUpdateRequest.getStatus().name()));
        
        transaction = transactionRepository.save(transaction);
        return new TransactionResponseAdapter(transaction);
    }

    @Override
    public StatusResponseDto getTransactionStatus(UUID transactionId) {
        // Implementation to get transaction status
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException("Transaction not found with id: " + transactionId));
        
        return new StatusResponseAdapter(transaction);
    }
}
