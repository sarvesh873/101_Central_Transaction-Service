package com.central.transaction_service.grpc;

import com.central.transaction.TransactionServiceGrpc;
import com.central.transaction_service.service.TransactionService;
import com.central.transaction.*;
import com.google.protobuf.Timestamp;
import com.google.rpc.Code;
import com.google.rpc.Status;
import io.grpc.protobuf.StatusProto;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.openapitools.model.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.dao.DataIntegrityViolationException;
import com.central.transaction_service.exception.*;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class GrpcTransactionServiceImpl extends TransactionServiceGrpc.TransactionServiceImplBase {

    private final TransactionService transactionService;

    @Override
    public void createTransaction(TransactionRequestGRPC request, StreamObserver<TransactionResponseGRPC> responseObserver) {
        try {
            TransactionRequest transactionRequest = new TransactionRequest();
            transactionRequest.setSenderId(request.getSenderId());
            transactionRequest.setReceiverId(request.getReceiverId());
            transactionRequest.setAmount(request.getAmount());
            transactionRequest.setDescription(request.getDescription());

            TransactionResponse serviceResponse = transactionService.createTransaction(transactionRequest);
            responseObserver.onNext(convertToTransactionResponseGRPC(serviceResponse));
            responseObserver.onCompleted();
        } catch (DataIntegrityViolationException e) {
            log.error("Error creating transaction: {}", e.getMessage());
            handleError(responseObserver, Code.INVALID_ARGUMENT, "Invalid data provided for transaction.");
        } catch (Exception e) {
            log.error("Unexpected error creating transaction: {}", e.getMessage());
            handleError(responseObserver, Code.INTERNAL, "An unexpected error occurred.");
        }
    }

    @Override
    public void getUserTransactions(GetUserTransactionsRequestGRPC request, StreamObserver<GetUserTransactionsResponseGRPC> responseObserver) {
        try {
            // Adjust for 0-indexed pages
            int page = request.getPage() > 0 ? request.getPage() - 1 : 0;
            // Default sorting
            String sortByField = "initiatedAt";
            if (request.hasSortBy() && !request.getSortBy().isEmpty()) {
                sortByField = request.getSortBy();
            }

            // The 'sort_desc' field defaults to true in the API spec.
            Sort.Direction direction = request.getSortDesc() ? Sort.Direction.DESC : Sort.Direction.ASC;

            Pageable pageable = PageRequest.of(page, request.getPageSize(), Sort.by(direction, sortByField));

            Page<TransactionResponse> transactionPage = transactionService.getUserTransactions(
                    request.getUserCode(),
                    request.hasStatusFilter() ? request.getStatusFilter().name() : null,
                    toOffsetDateTime(request.getFromDate()),
                    toOffsetDateTime(request.getToDate()),
                    pageable
            );

            GetUserTransactionsResponseGRPC.Builder responseBuilder = GetUserTransactionsResponseGRPC.newBuilder();
            transactionPage.getContent().forEach(transactionResponse -> responseBuilder.addItems(convertToTransactionResponseGRPC(transactionResponse)));

            // Create and set pagination response
            PaginationResponse pagination = new PaginationResponse();
            pagination.setTotalItems(transactionPage.getTotalElements());
            pagination.setTotalPages(transactionPage.getTotalPages());
            pagination.setCurrentPage(transactionPage.getNumber() + 1);
            pagination.setPageSize(transactionPage.getSize());
            pagination.setHasNext(transactionPage.hasNext());
            pagination.setHasPrevious(transactionPage.hasPrevious());
            responseBuilder.setPagination(convertToPaginationResponseGRPC(pagination));

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error getting user transactions: {}", e.getMessage());
            handleError(responseObserver, Code.INTERNAL, "An unexpected error occurred while fetching user transactions.");
        }
    }

    @Override
    public void getTransactionDetails(TransactionIdRequestGRPC request, StreamObserver<TransactionResponseGRPC> responseObserver) {
        try {
            TransactionResponse serviceResponse = transactionService.getTransactionDetails(java.util.UUID.fromString(request.getTransactionId()));
            responseObserver.onNext(convertToTransactionResponseGRPC(serviceResponse));
            responseObserver.onCompleted();
        } catch (TransactionNotFoundException e) {
            log.warn("Transaction not found: {}", request.getTransactionId());
            handleError(responseObserver, Code.NOT_FOUND, e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid transaction ID format: {}", request.getTransactionId());
            handleError(responseObserver, Code.INVALID_ARGUMENT, "Invalid transaction ID format.");
        } catch (Exception e) {
            log.error("Error getting transaction details: {}", e.getMessage());
            handleError(responseObserver, Code.INTERNAL, "An unexpected error occurred while fetching transaction details.");
        }
    }

    @Override
    public void updateTransactionStatus(UpdateTransactionRequestGRPC request, StreamObserver<TransactionResponseGRPC> responseObserver) {
        try {
            StatusUpdateRequest statusUpdateRequest = new StatusUpdateRequest();
            statusUpdateRequest.setStatus(OverallStatusEnum.valueOf(request.getUpdateData().getStatus().name()));

            TransactionResponse serviceResponse = transactionService.updateTransactionStatus(
                    java.util.UUID.fromString(request.getTransactionId()),
                    statusUpdateRequest
            );

            responseObserver.onNext(convertToTransactionResponseGRPC(serviceResponse));
            responseObserver.onCompleted();
        } catch (TransactionNotFoundException e) {
            log.warn("Transaction not found for update: {}", request.getTransactionId());
            handleError(responseObserver, Code.NOT_FOUND, e.getMessage());
        } catch (InvalidTransactionStatusException e) {
            log.warn("Invalid status update for transaction: {}", request.getTransactionId());
            handleError(responseObserver, Code.INVALID_ARGUMENT, e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid argument for transaction update: {}", e.getMessage());
            handleError(responseObserver, Code.INVALID_ARGUMENT, "Invalid transaction ID or status format.");
        } catch (Exception e) {
            log.error("Error updating transaction status: {}", e.getMessage());
            handleError(responseObserver, Code.INTERNAL, "An unexpected error occurred while updating transaction status.");
        }
    }

    @Override
    public void getTransactionStatus(TransactionIdRequestGRPC request, StreamObserver<StatusResponseGRPC> responseObserver) {
        try {
            StatusResponse serviceResponse = transactionService.getTransactionStatus(java.util.UUID.fromString(request.getTransactionId()));

            StatusResponseGRPC grpcResponse = StatusResponseGRPC.newBuilder()
                    .setTransactionId(serviceResponse.getTransactionId().toString())
                    .setStatus(convertToOverallStatusGRPC(serviceResponse.getStatus()))
                    .build();

            responseObserver.onNext(grpcResponse);
            responseObserver.onCompleted();
        } catch (TransactionNotFoundException e) {
            log.warn("Transaction not found for status check: {}", request.getTransactionId());
            handleError(responseObserver, Code.NOT_FOUND, e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid transaction ID format for status check: {}", request.getTransactionId());
            handleError(responseObserver, Code.INVALID_ARGUMENT, "Invalid transaction ID format.");
        } catch (Exception e) {
            log.error("Error getting transaction status: {}", e.getMessage());
            handleError(responseObserver, Code.INTERNAL, "An unexpected error occurred while fetching transaction status.");
        }
    }

    // Helper methods
    private OverallStatusGRPC convertToOverallStatusGRPC(OverallStatusEnum statusEnum) {
        if (statusEnum == null) {
            return OverallStatusGRPC.STATUS_UNSPECIFIED;
        }
        try {
            return OverallStatusGRPC.valueOf(statusEnum.name());
        } catch (IllegalArgumentException e) {
            log.warn("Unrecognized status value: {}", statusEnum.name());
            return OverallStatusGRPC.STATUS_UNSPECIFIED;
        }
    }

    private TransactionResponseGRPC convertToTransactionResponseGRPC(TransactionResponse response) {
        if (response == null) {
            return TransactionResponseGRPC.getDefaultInstance();
        }

        TransactionResponseGRPC.Builder builder = TransactionResponseGRPC.newBuilder();
        if (response.getTransactionId() != null) builder.setTransactionId(response.getTransactionId().toString());
        if (response.getSenderId() != null) builder.setSenderId(response.getSenderId());
        if (response.getReceiverId() != null) builder.setReceiverId(response.getReceiverId());
        if (response.getAmount() != null) builder.setAmount(response.getAmount());
        if (response.getDescription() != null) builder.setDescription(response.getDescription());
        if (response.getStatus() != null) builder.setStatus(convertToOverallStatusGRPC(response.getStatus()));
        if (response.getCreatedAt() != null) builder.setCreatedAt(convertToTimestamp(response.getCreatedAt()));
        if (response.getUpdatedAt() != null) builder.setUpdatedAt(convertToTimestamp(response.getUpdatedAt()));

        return builder.build();
    }

    private PaginationResponseGRPC convertToPaginationResponseGRPC(PaginationResponse pagination) {
        if (pagination == null) {
            return PaginationResponseGRPC.getDefaultInstance();
        }
        PaginationResponseGRPC.Builder builder = PaginationResponseGRPC.newBuilder();
        if (pagination.getTotalItems() != null) builder.setTotalItems(pagination.getTotalItems());
        if (pagination.getTotalPages() != null) builder.setTotalPages(pagination.getTotalPages());
        if (pagination.getCurrentPage() != null) builder.setCurrentPage(pagination.getCurrentPage());
        if (pagination.getPageSize() != null) builder.setPageSize(pagination.getPageSize());
        if (pagination.getHasNext() != null) builder.setHasNext(pagination.getHasNext());
        if (pagination.getHasPrevious() != null) builder.setHasPrevious(pagination.getHasPrevious());
        return builder.build();
    }

    private Timestamp convertToTimestamp(OffsetDateTime offsetDateTime) {
        if (offsetDateTime == null) {
            return Timestamp.getDefaultInstance();
        }
        Instant instant = offsetDateTime.toInstant();
        return Timestamp.newBuilder()
                .setSeconds(instant.getEpochSecond())
                .setNanos(instant.getNano())
                .build();
    }

    private OffsetDateTime toOffsetDateTime(Timestamp timestamp) {
        if (timestamp == null || (timestamp.getSeconds() == 0 && timestamp.getNanos() == 0)) {
            return null;
        }
        return OffsetDateTime.ofInstant(
                Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos()),
                ZoneOffset.UTC
        );
    }

    private <T> void handleError(StreamObserver<T> responseObserver, Code code, String message) {
        Status status = Status.newBuilder()
                .setCode(code.getNumber())
                .setMessage(message)
                .build();
        responseObserver.onError(StatusProto.toStatusRuntimeException(status));
    }
}
