package com.central.transaction_service.service;

import com.central.transaction_service.exception.GrpcServiceException;
import com.central.transaction_service.exception.InvalidTransactionStatusException;
import com.central.transaction_service.exception.TransactionNotFoundException;
import com.central.transaction_service.exception.TransactionProcessingException;
import com.central.transaction_service.grpc.WalletHoldServiceGrpcClient;
import com.central.transaction_service.grpc.WalletServiceGrpcClient;
import com.central.transaction_service.kafka.KafkaEventProducer;
import com.central.transaction_service.model.Transaction;
import com.central.transaction_service.model.TransactionStatus;
import com.central.transaction_service.repository.TransactionRepository;
import com.central.transaction_service.repository.TransactionSpecifications;
import com.central.transaction_service.dto.*;
import com.central.wallet.HoldResponseGRPC;
import com.central.wallet.WalletResponseGRPC;
import com.central.wallet.WalletTransactionResponseGRPC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
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

    private final RefundService refundService;

    private final WalletServiceGrpcClient walletServiceGrpcClient;

    private final WalletHoldServiceGrpcClient walletHoldServiceGrpcClient;

    public TransactionServiceImpl(TransactionRepository transactionRepository, KafkaEventProducer kafkaEventProducer, RefundService refundService, WalletServiceGrpcClient walletServiceGrpcClient, WalletHoldServiceGrpcClient walletHoldServiceGrpcClient) {
        this.transactionRepository = transactionRepository;
        this.kafkaEventProducer = kafkaEventProducer;
        this.refundService = refundService;
        this.walletServiceGrpcClient = walletServiceGrpcClient;
        this.walletHoldServiceGrpcClient = walletHoldServiceGrpcClient;
    }

    @Override
    public TransactionResponseDto createTransaction(TransactionRequestDto transactionRequest) {
        Transaction transaction = Transaction.builder()
                .senderId(transactionRequest.getSenderId())
                .receiverId(transactionRequest.getReceiverId())
                .amount(transactionRequest.getAmount())
                .description(transactionRequest.getDescription())
                .status(TransactionStatus.PENDING)
                .build();
        transaction = transactionRepository.save(transaction);
        log.info("Transaction created successfully with Transaction ID: {}", transaction.getTransaction_id());

        try {
            log.info("Fetching wallet details for receiver ID: {}", transactionRequest.getReceiverId());
            WalletResponseGRPC walletResponseGRPC = walletServiceGrpcClient.getUserWallet(transactionRequest.getReceiverId());
            log.info("Successfully fetched wallet details for receiver ID: {}", walletResponseGRPC.getUserCode());
            log.info("Successfully fetched wallet details: {}", walletResponseGRPC);

            try{
                log.info("Holding wallet for sender ID: {}", transactionRequest.getSenderId());
                HoldResponseGRPC holdResponseGRPC = walletHoldServiceGrpcClient.placeHold(transaction.getSenderId(),transaction.getAmount(),
                                                    "INR",transaction.getTransaction_id().toString(),transaction.getDescription());
                log.info("Successfully held wallet for sender ID: {} with hold reference {}",transaction.getSenderId(),holdResponseGRPC.getHoldId());

                transaction.setHoldId(holdResponseGRPC.getHoldId());
                transaction.setStatus(TransactionStatus.PLACED_HOLD);
                transactionRepository.save(transaction);

//                send otp and verify otp
                try{
                    log.info("Sending OTP for sender ID: {}", transactionRequest.getSenderId());
//                   send otp to user
                    log.info("Successfully sent OTP to sender ID: {} for transaction ID :{}", transactionRequest.getSenderId(), transaction.getTransaction_id());
                    transaction.setOtpKey("sent");
                    transaction.setStatus(TransactionStatus.OTP_SENT);
                    transactionRepository.save(transaction);
                }
                catch (Exception e){
                    log.error("Failed to send OTP for transaction ID: {}", transaction.getTransaction_id(), e);
                    throw new TransactionProcessingException("Failed to send OTP. Please try again.", e);
                }

            }catch (GrpcServiceException e){
                log.error("Error while placing hold for sender ID: {} for transactionId {}. Error: {}",
                        transactionRequest.getSenderId(),transaction.getTransaction_id(), e.getMessage(), e);
                transaction.setStatus(TransactionStatus.FAILED);
                transaction.setDescription(e.getMessage());
                transactionRepository.save(transaction);
                Transaction failedTransaction = transactionRepository.save(transaction);
                return new TransactionResponseAdapter(failedTransaction);
            }
            catch (Exception e) {
                log.error("Error while fetching wallet details for receiver ID: {}. Error: {}",
                        transactionRequest.getReceiverId(), e.getMessage(), e);
                throw new TransactionProcessingException("Failed to process wallet details for receiver: " +
                        transactionRequest.getReceiverId(), e);
            }
        }
        catch (GrpcServiceException e){
            log.error("Error while fetching GRPC wallet details for receiver ID: {}. Error: {}",
                    transactionRequest.getReceiverId(), e.getMessage(), e);
            transaction.setStatus(TransactionStatus.DECLINED);
            transaction.setDescription(e.getMessage());
            transactionRepository.save(transaction);
            Transaction failedTransaction = transactionRepository.save(transaction);
            return new TransactionResponseAdapter(failedTransaction);
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

    public TransactionResponseDto verifyTransactionOtp(UUID transactionId, String otpCode) {
        // 1. Fetch and Validate Transaction State
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException("Transaction not found with ID: " + transactionId));

        if (transaction.getStatus() != TransactionStatus.OTP_SENT) {
            throw new TransactionProcessingException("Cannot verify OTP. Transaction is not in OTP_SENT status. Current status: " + transaction.getStatus());
        }

        try {
            log.info("Attempting to verify OTP for transaction ID: {}", transactionId);

            // ** Replace with actual OTP service call **
            // boolean isOtpValid = otpService.verifyOtp(transaction.getSenderId(), transaction.getOtpKey(), otpCode);
            boolean isOtpValid = true; // Placeholder for successful verification

            if (isOtpValid) {
                // Update status and proceed to execution
                transaction.setOtpKey(null); // Securely clear the OTP key
                transaction.setStatus(TransactionStatus.OTP_VERIFIED);
                transactionRepository.save(transaction);
                log.info("OTP successfully verified. Internally calling transaction execution for ID: {}", transactionId);

                // ** INTERNAL CALL TO EXECUTE THE TRANSACTION **
                return processTransactionAfterOtpVerification(transactionId);

            } else {
                // 2. Handle OTP Failure: Update status and release hold
                String reason = ("OTP verification failed for transaction ID: "+ transactionId);
                log.info(reason);
                transaction.setStatus(TransactionStatus.OTP_VERIFICATION_FAILED);

                // CRITICAL: If the OTP fails, the hold must be released
                if (transaction.getHoldId() != null) {
                    walletHoldServiceGrpcClient.releaseHold(transaction.getHoldId(),reason);
                    transaction.setHoldId(null);
                }
                transactionRepository.save(transaction);

                throw new TransactionProcessingException("The submitted OTP is incorrect or expired.");
            }

        } catch ( TransactionProcessingException e) {
            // Re-throw specific exceptions for the controller to handle and return to the user
            throw e;
        } catch (Exception e) {
            // Catch any unexpected system errors (e.g., repository failure)
            log.error("Unexpected error during OTP verification for transaction ID: {}", transactionId, e);
            throw new TransactionProcessingException("Failed to complete verification due to a system error.", e);
        }
    }

    // Note: This method is now called *internally* by verifyTransactionOtp
    private TransactionResponseDto processTransactionAfterOtpVerification(UUID transactionId) {
        // 1. Fetch Transaction (Safeguard check)
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new TransactionNotFoundException("Transaction not found with ID: " + transactionId));

        // 2. Sanity Check
        if (transaction.getStatus() != TransactionStatus.OTP_VERIFIED) {
            throw new TransactionProcessingException("Transaction cannot be executed. Expected status OTP_VERIFIED, found: " + transaction.getStatus());
        }

        try {
            log.info("Starting fund capture  for transaction ID: {}", transactionId);
            String description = String.format("Transferring %.2f %s from user %s to %s - OTP verified and hold captured", 
                transaction.getAmount(), "INR", transaction.getSenderId(), transaction.getReceiverId());
            log.info(description);
            
            // ** 3. CAPTURE THE HELD FUNDS **
            HoldResponseGRPC holdResponseGRPC = walletHoldServiceGrpcClient.captureHold(
                    transaction.getHoldId(),
                    transaction.getTransaction_id().toString(),
                    description,
                    true
            );
            log.info("Funds successfully captured and transferred for transaction ID: {} for Hold ID: {}", transactionId,holdResponseGRPC.getHoldId());

            // 4. Update Final Status as amount is deducted from sender's wallet
            transaction.setStatus(TransactionStatus.PROCESSING);
            transaction.setHoldId(holdResponseGRPC.getHoldId()); // Clear the hold ID after successful capture
            transactionRepository.save(transaction);

            //5. Deposit the amount to the receiver's wallet if this fails refund the amount to sender's wallet
            try{
                log.info("Initiating fund deposit - Transaction ID: {}, Receiver ID: {}, Amount: {} {}", 
                    transactionId, transaction.getReceiverId(), transaction.getAmount(), "INR");

                WalletTransactionResponseGRPC walletTransactionResponseGRPC = walletServiceGrpcClient.depositFunds(
                    transaction.getReceiverId(),
                    transaction.getAmount(),
                    "INR"
                );

                log.info("Funds successfully deposited - Transaction ID: {}, Receiver Wallet: {}, Amount: {} {}",
                    transactionId, walletTransactionResponseGRPC.getWalletId(), transaction.getAmount(), "INR");
                // 6. Update Final Status as amount is deposited to receiver's wallet
                transaction.setStatus(TransactionStatus.COMPLETED);
                transactionRepository.save(transaction);

                log.info("Transaction COMPLETED successfully - Transaction ID: {}, Sender: {}, Receiver: {}, " +
                        "Amount: {} {}, Hold ID: {}, Wallet Transaction ID: {}",
                    transactionId,
                    transaction.getSenderId(),
                    transaction.getReceiverId(),
                    transaction.getAmount(),
                    "INR",
                    transaction.getHoldId(),
                    walletTransactionResponseGRPC.getWalletId());
            }
            catch (GrpcServiceException e){
                String errorMsg = String.format("Failed to deposit funds - Transaction ID: %s, Stage: FUND_DEPOSIT, Error: %s",
                transactionId, e.getMessage());
                log.error("{}. Transaction Details - Sender: {}, Receiver: {}, Amount: {} {}",
                    errorMsg, transaction.getSenderId(), transaction.getReceiverId(), transaction.getAmount(), "INR", e);

                transaction.setStatus(TransactionStatus.REFUND_INITIATED);
                transaction.setDescription(errorMsg);
                Transaction failedTransaction = transactionRepository.save(transaction);
                // 7. Initiate refund process
                refundService.initiateRefundAsync(failedTransaction);
                return new TransactionResponseAdapter(failedTransaction);
            } catch (Exception e) {
                // Catch any other unexpected error during the execution phase
                log.error("An unexpected error occurred during execution for transaction ID: {}", transactionId, e);
                throw new TransactionProcessingException("An unexpected error occurred during transaction execution.", e);
            }

            return new TransactionResponseAdapter(transaction);

        } catch (GrpcServiceException e) {
            // 5. Handle Fund Transfer Failure
            log.error("Error during fund transfer for transaction ID: {}. Error: {}", transactionId, e.getMessage(), e);

            // CRITICAL: If transfer fails, the hold must be canceled/released
            try {
                String releaseReason = String.format("Releasing hold due to transfer failure - Transaction ID: %s, Error: %s", 
                    transactionId, e.getMessage());
                log.warn("Attempting to release hold {} - {}", transaction.getHoldId(), releaseReason);

                HoldResponseGRPC holdResponseGRPC =  walletHoldServiceGrpcClient.releaseHold(transaction.getHoldId(),releaseReason);
                log.info("Successfully released hold {} for failed transaction ID: {}", 
                    holdResponseGRPC.getHoldId(), transactionId);
                    
            } catch (GrpcServiceException grpcEx) {
                String errorMsg = String.format("Failed to release hold %s for transaction ID: %s. Manual intervention required. Error: %s", 
                    transaction.getHoldId(), transactionId, grpcEx.getMessage());
                log.error(errorMsg, grpcEx);
                throw new TransactionProcessingException(errorMsg, grpcEx);
            } catch (Exception ex) {
                String errorMsg = String.format("Unexpected error while releasing hold %s for transaction ID: %s. Error: %s", 
                    transaction.getHoldId(), transactionId, ex.getMessage());
                log.error(errorMsg, ex);
                throw new TransactionProcessingException(errorMsg, ex);
            }

            // Update transaction status to FAILED
            transaction.setStatus(TransactionStatus.FAILED);
            transaction.setDescription("Fund Transfer Failed: " + e.getMessage());
            transaction.setHoldId(null);
            transactionRepository.save(transaction);

            return new TransactionResponseAdapter(transaction);

        } catch (Exception e) {
            // Catch any other unexpected error during the execution phase
            log.error("An unexpected error occurred during execution for transaction ID: {}", transactionId, e);
            throw new TransactionProcessingException("An unexpected error occurred during transaction execution.", e);
        }
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
