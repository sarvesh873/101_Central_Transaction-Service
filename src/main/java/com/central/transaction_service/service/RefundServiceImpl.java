package com.central.transaction_service.service;

import com.central.transaction_service.exception.TransactionProcessingException;
import com.central.transaction_service.grpc.WalletHoldServiceGrpcClient;
import com.central.transaction_service.grpc.WalletServiceGrpcClient;
import com.central.transaction_service.model.Transaction;
import com.central.transaction_service.model.TransactionStatus;
import com.central.transaction_service.repository.TransactionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Slf4j
@Service
public class RefundServiceImpl implements RefundService {
    private final TransactionRepository transactionRepository;
    private final WalletServiceGrpcClient walletServiceGrpcClient;
    private final Executor virtualThreadExecutor;

    public RefundServiceImpl(
            TransactionRepository transactionRepository,
            WalletHoldServiceGrpcClient walletHoldServiceGrpcClient,
            WalletServiceGrpcClient walletServiceGrpcClient,
            @Qualifier("ioTaskExecutor") Executor virtualThreadExecutor) {

        this.transactionRepository = transactionRepository;
        this.walletServiceGrpcClient = walletServiceGrpcClient;
        this.virtualThreadExecutor = virtualThreadExecutor;
    }

    @Override
    @Transactional
    public CompletableFuture<Void> initiateRefundAsync(Transaction transaction) {
        return CompletableFuture.runAsync(() -> {
            try {
                processRefund(transaction);
            } catch (Exception e) {
                log.error("Failed to process refund asynchronously for transaction ID: {}",
                        transaction.getTransaction_id(), e);
                throw e;
            }
        }, virtualThreadExecutor);
    }

    protected void processRefund(Transaction transaction) {
        try {
            log.info("Initiating refund - Transaction ID: {}, Sender: {}, Amount: {} {}",
                    transaction.getTransaction_id(), transaction.getSenderId(),
                    transaction.getAmount(), "INR");

            String refundDescription = String.format("Refund for failed transaction %s", transaction.getTransaction_id());

            var refundResponse = walletServiceGrpcClient.depositFunds(
                    transaction.getSenderId(),
                    transaction.getAmount(),
                    "INR"
            );

            transaction.setStatus(TransactionStatus.REFUNDED);
            transaction.setDescription(refundDescription);
            transactionRepository.save(transaction);

            log.info("Refund completed successfully - Transaction ID: {}, Wallet Txn ID: {}",
                    transaction.getTransaction_id(), refundResponse.getWalletId());

        } catch (Exception e) {
            String errorMsg = String.format("Refund failed for transaction ID: %s. Error: %s",
                    transaction.getTransaction_id()
                    , e.getMessage());
            log.error(errorMsg, e);

            transaction.setStatus(TransactionStatus.REFUND_FAILED);
            transaction.setDescription(errorMsg);
            transactionRepository.save(transaction);

            throw new TransactionProcessingException(errorMsg, e);
        }
    }
}