package com.central.transaction_service.grpc;

import com.central.transaction_service.exception.GrpcServiceException;
import com.central.wallet.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Status;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import javax.annotation.PreDestroy;
@Service
@Slf4j
public class WalletServiceGrpcClient extends BaseGrpcClient {
    private final WalletServiceGrpc.WalletServiceBlockingStub walletServiceBlockingStub;

    public WalletServiceGrpcClient(
            @Value("${wallet.service.host}") String walletServiceHost,
            @Value("${wallet.service.port}") int walletServicePort,
            @Qualifier("ioTaskExecutor") ExecutorService ioTaskExecutor) {

        super("Wallet", walletServiceHost, walletServicePort, ioTaskExecutor);
        this.walletServiceBlockingStub = WalletServiceGrpc.newBlockingStub(channel)
                .withExecutor(ioTaskExecutor);
    }

    @PreDestroy
    public void shutdown() {
        super.shutdown();
    }

    public WalletTransactionResponseGRPC depositFunds(String userCode, double amount, String currency) {
        try {
            return CompletableFuture.supplyAsync(() -> {
                WalletTransactionRequestGRPC request = WalletTransactionRequestGRPC.newBuilder()
                        .setUserCode(userCode)
                        .setAmount(amount)
                        .setCurrency(currency)
                        .build();
                return walletServiceBlockingStub
                        .withDeadlineAfter(5, TimeUnit.SECONDS)
                        .depositFunds(request);
            }, ioTaskExecutor).join();
        } catch (CompletionException e) {
            Throwable cause = e.getCause();
            Status status = Status.fromThrowable(cause);
            log.error("Error in depositFunds for user {}: {}", userCode, cause.getMessage());
            throw new GrpcServiceException(status,"Failed to process deposit", cause);
        }
    }

    public WalletTransactionResponseGRPC withdrawFunds(String userCode, double amount, String currency) {
        try {
            return CompletableFuture.supplyAsync(() -> {
                WalletTransactionRequestGRPC request = WalletTransactionRequestGRPC.newBuilder()
                        .setUserCode(userCode)
                        .setAmount(amount)
                        .setCurrency(currency)
                        .build();
                return walletServiceBlockingStub
                        .withDeadlineAfter(5, TimeUnit.SECONDS)
                        .withdrawFunds(request);
            }, ioTaskExecutor).join();
        } catch (CompletionException e) {
            Throwable cause = e.getCause();
            Status status = Status.fromThrowable(cause);
            log.error("Error in withdrawFunds for user {}: {}", userCode, cause.getMessage());
            throw new GrpcServiceException(status,"Failed to process withdrawal", cause);
        }
    }

    public WalletResponseGRPC getUserWallet(String userCode) {
        try {
            return CompletableFuture.supplyAsync(() -> {
                GetWalletRequestGRPC request = GetWalletRequestGRPC.newBuilder()
                        .setUserCode(userCode)
                        .build();
                return walletServiceBlockingStub
                        .withDeadlineAfter(5, TimeUnit.SECONDS)
                        .getUserWallet(request);
            }, ioTaskExecutor).join();
        } catch (CompletionException e) {
            Throwable cause = e.getCause();
            Status status = Status.fromThrowable(cause);
            log.error("Error in getUserWallet for user {}: {}", userCode, cause.getMessage());
            throw new GrpcServiceException(status,"Failed to get user wallet", cause);
        }
    }

}
