package com.central.transaction_service.grpc;

import com.central.transaction_service.exception.GrpcServiceException;
import com.central.wallet.*;
import com.google.protobuf.Timestamp;
import io.grpc.Status;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class WalletHoldServiceGrpcClient extends BaseGrpcClient {
    private final HoldServiceGrpc.HoldServiceBlockingStub holdServiceBlockingStub;

    public WalletHoldServiceGrpcClient(
            @Value("${wallet.service.host}") String walletServiceHost,
            @Value("${wallet.service.port}") int walletServicePort,
            @Qualifier("ioTaskExecutor") ExecutorService ioTaskExecutor) {
        
        super("Wallet Hold", walletServiceHost, walletServicePort, ioTaskExecutor);
        this.holdServiceBlockingStub = HoldServiceGrpc.newBlockingStub(channel)
                .withExecutor(ioTaskExecutor);
    }
    
    @PreDestroy
    public void shutdown() {
        super.shutdown();
    }

    public HoldResponseGRPC placeHold(String userCode, double amount, String currency,
                                      String transactionId, String description) {
        try {
            return CompletableFuture.supplyAsync(() -> {
                        
                PlaceHoldRequestGRPC request = PlaceHoldRequestGRPC.newBuilder()
                        .setUserCode(userCode)
                        .setAmount(amount)
                        .setCurrency(currency)
                        .setTransactionId(transactionId)
                        .setDescription(description)
                        .build();
                        
                return holdServiceBlockingStub
                        .withDeadlineAfter(5, TimeUnit.SECONDS)
                        .placeHold(request);
            }, ioTaskExecutor).join();
        } catch (CompletionException e) {
            Throwable cause = e.getCause();
            Status status = Status.fromThrowable(cause);
            log.error("Error placing hold for user {}: {}", userCode, cause.getMessage());
            throw new GrpcServiceException(status, "Failed to place hold", cause);
        }
    }

    public HoldResponseGRPC captureHold(String holdId, String transactionId, String description, boolean releaseRemainder) {
        try {
            return CompletableFuture.supplyAsync(() -> {
                CaptureHoldRequestGRPC request = CaptureHoldRequestGRPC.newBuilder()
                        .setHoldId(holdId)
                        .setTransactionId(transactionId)
                        .setDescription(description)
                        .setReleaseRemainder(releaseRemainder)
                        .build();
                        
                return holdServiceBlockingStub
                        .withDeadlineAfter(5, TimeUnit.SECONDS)
                        .captureHold(request);
            }, ioTaskExecutor).join();
        } catch (CompletionException e) {
            Throwable cause = e.getCause();
            Status status = Status.fromThrowable(cause);
            log.error("Error capturing hold {}: {}", holdId, cause.getMessage());
            throw new GrpcServiceException(status, "Failed to capture hold", cause);
        }
    }

    public HoldResponseGRPC releaseHold(String holdId, String reason) {
        try {
            return CompletableFuture.supplyAsync(() -> {
                ReleaseHoldRequestGRPC request = ReleaseHoldRequestGRPC.newBuilder()
                        .setHoldId(holdId)
                        .setReason(reason)
                        .build();
                        
                return holdServiceBlockingStub
                        .withDeadlineAfter(5, TimeUnit.SECONDS)
                        .releaseHold(request);
            }, ioTaskExecutor).join();
        } catch (CompletionException e) {
            Throwable cause = e.getCause();
            Status status = Status.fromThrowable(cause);
            log.error("Error releasing hold {}: {}", holdId, cause.getMessage());
            throw new GrpcServiceException(status, "Failed to release hold", cause);
        }
    }

    public HoldResponseGRPC getHold(String holdId) {
        try {
            return CompletableFuture.supplyAsync(() -> {
                GetHoldRequestGRPC request = GetHoldRequestGRPC.newBuilder()
                        .setHoldId(holdId)
                        .build();
                        
                return holdServiceBlockingStub
                        .withDeadlineAfter(5, TimeUnit.SECONDS)
                        .getHold(request);
            }, ioTaskExecutor).join();
        } catch (CompletionException e) {
            Throwable cause = e.getCause();
            Status status = Status.fromThrowable(cause);
            log.error("Error getting hold {}: {}", holdId, cause.getMessage());
            throw new GrpcServiceException(status, "Failed to get hold", cause);
        }
    }

    public ListHoldsResponseGRPC listHolds(
            String userCode,
            String status,
            String currency,
            LocalDateTime fromDate,
            LocalDateTime toDate,
            int page,
            int pageSize) {

        try {
            ListHoldsRequestGRPC.Builder requestBuilder = ListHoldsRequestGRPC.newBuilder()
                    .setUserCode(userCode);
            if (status != null) {
                requestBuilder.setStatus(status);
            }
            if (currency != null) {
                requestBuilder.setCurrency(currency);
            }
            if (fromDate != null) {
                requestBuilder.setFromDate(Timestamp.newBuilder()
                        .setSeconds(fromDate.toEpochSecond(ZoneOffset.UTC))
                        .setNanos(fromDate.getNano())
                        .build());
            }
            if (toDate != null) {
                requestBuilder.setToDate(Timestamp.newBuilder()
                        .setSeconds(toDate.toEpochSecond(ZoneOffset.UTC))
                        .setNanos(toDate.getNano())
                        .build());
            }

            PaginationRequestGRPC pagination = PaginationRequestGRPC.newBuilder()
                    .setPage(page)
                    .setPageSize(pageSize)
                    .build();
            requestBuilder.setPagination(pagination);

            return CompletableFuture.supplyAsync(() ->
                            holdServiceBlockingStub
                                    .withDeadlineAfter(5, TimeUnit.SECONDS)
                                    .listHolds(requestBuilder.build())
                    , ioTaskExecutor).join();
        } catch (CompletionException e) {
            Throwable cause = e.getCause();
            Status resstatus = Status.fromThrowable(cause);
            log.error("Error listing holds: {}", cause.getMessage());
            throw new GrpcServiceException(resstatus, "Failed to list holds", cause);
        }
    }

    public HoldResponseGRPC extendHold(String holdId, LocalDateTime newExpiresAt, String reason) {
        try {
            return CompletableFuture.supplyAsync(() -> {
                Timestamp timestamp = Timestamp.newBuilder()
                        .setSeconds(newExpiresAt.toEpochSecond(ZoneOffset.UTC))
                        .setNanos(newExpiresAt.getNano())
                        .build();
                        
                ExtendHoldRequestGRPC request = ExtendHoldRequestGRPC.newBuilder()
                        .setHoldId(holdId)
                        .setNewExpiresAt(timestamp)
                        .setReason(reason)
                        .build();
                        
                return holdServiceBlockingStub
                        .withDeadlineAfter(5, TimeUnit.SECONDS)
                        .extendHold(request);
            }, ioTaskExecutor).join();
        } catch (CompletionException e) {
            Throwable cause = e.getCause();
            Status status = Status.fromThrowable(cause);
            log.error("Error extending hold {}: {}", holdId, cause.getMessage());
            throw new GrpcServiceException(status, "Failed to extend hold", cause);
        }
    }

    public HoldResponseGRPC adjustHold(String holdId, double newAmount, String reason) {
        try {
            return CompletableFuture.supplyAsync(() -> {
                AdjustHoldRequestGRPC request = AdjustHoldRequestGRPC.newBuilder()
                        .setHoldId(holdId)
                        .setNewAmount(newAmount)
                        .setReason(reason)
                        .build();
                        
                return holdServiceBlockingStub
                        .withDeadlineAfter(5, TimeUnit.SECONDS)
                        .adjustHold(request);
            }, ioTaskExecutor).join();
        } catch (CompletionException e) {
            Throwable cause = e.getCause();
            Status status = Status.fromThrowable(cause);
            log.error("Error adjusting hold {}: {}", holdId, cause.getMessage());
            throw new GrpcServiceException(status, "Failed to adjust hold", cause);
        }
    }
}
