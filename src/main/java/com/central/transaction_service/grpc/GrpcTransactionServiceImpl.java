package com.central.transaction_service.grpc;

import com.central.transaction.*;
import com.central.transaction.TransactionServiceGrpc;
import com.central.transaction_service.dto.*;
import com.central.transaction_service.exception.InvalidTransactionStatusException;
import com.central.transaction_service.exception.TransactionNotFoundException;
import com.central.transaction_service.exception.TransactionProcessingException;
import com.central.transaction_service.service.TransactionService;
import com.google.protobuf.Timestamp;
import com.google.rpc.Code;
import com.google.rpc.Status;
import io.grpc.protobuf.StatusProto;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class GrpcTransactionServiceImpl extends TransactionServiceGrpc.TransactionServiceImplBase {

    private final TransactionService transactionService;

    @Override
    public void createTransaction(TransactionRequestGRPC request, StreamObserver<TransactionResponseGRPC> responseObserver) {
        try {
            TransactionRequestDto requestDto = new GrpcTransactionRequestAdapter(request);
            TransactionResponseDto serviceResponse = transactionService.createTransaction(requestDto);
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

            Page<TransactionResponseDto> transactionPage = transactionService.getUserTransactions(
                    request.getUserCode(),
                    request.hasStatusFilter() ? request.getStatusFilter().name() : null,
                    toOffsetDateTime(request.getFromDate()),
                    toOffsetDateTime(request.getToDate()),
                    pageable
            );

            GetUserTransactionsResponseGRPC.Builder responseBuilder = GetUserTransactionsResponseGRPC.newBuilder();
            
            // Convert DTOs to gRPC responses
            transactionPage.getContent().forEach(dto -> 
                responseBuilder.addItems(convertToTransactionResponseGRPC(dto))
            );

            // Create and set pagination response
            PaginationResponseGRPC paginationResponse = PaginationResponseGRPC.newBuilder()
                .setTotalItems(transactionPage.getTotalElements())
                .setTotalPages(transactionPage.getTotalPages())
                .setCurrentPage(transactionPage.getNumber() + 1)
                .setPageSize(transactionPage.getSize())
                .setHasNext(transactionPage.hasNext())
                .setHasPrevious(transactionPage.hasPrevious())
                .build();
                
            responseBuilder.setPagination(paginationResponse);

            responseObserver.onNext(responseBuilder.build());
            responseObserver.onCompleted();
        } catch (Exception e) {
            log.error("Error getting user transactions: {}", e.getMessage(), e);
            handleError(responseObserver, Code.INTERNAL, "An unexpected error occurred while fetching user transactions.");
        }
    }

    @Override
    public void getTransactionDetails(TransactionIdRequestGRPC request, StreamObserver<TransactionResponseGRPC> responseObserver) {
        try {
            TransactionResponseDto serviceResponse = transactionService.getTransactionDetails(java.util.UUID.fromString(request.getTransactionId()));
            responseObserver.onNext(convertToTransactionResponseGRPC(serviceResponse));
            responseObserver.onCompleted();
        } catch (TransactionNotFoundException e) {
            log.warn("Transaction not found: {}", request.getTransactionId());
            handleError(responseObserver, Code.NOT_FOUND, e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid transaction ID format: {}", request.getTransactionId());
            handleError(responseObserver, Code.INVALID_ARGUMENT, "Invalid transaction ID format.");
        } catch (Exception e) {
            log.error("Error getting transaction details: {}", e.getMessage(), e);
            handleError(responseObserver, Code.INTERNAL, "An unexpected error occurred while fetching transaction details.");
        }
    }

    @Override
    public void updateTransactionStatus(UpdateTransactionRequestGRPC request, StreamObserver<TransactionResponseGRPC> responseObserver) {
        try {
            // Convert gRPC request to DTO using adapter
            StatusUpdateRequestDto statusUpdateDto = new GrpcStatusUpdateRequestAdapter(request);

            // Call service with DTO
            TransactionResponseDto serviceResponse = transactionService.updateTransactionStatus(
                    java.util.UUID.fromString(request.getTransactionId()),
                    statusUpdateDto
            );

            // Convert DTO to gRPC response
            responseObserver.onNext(convertToTransactionResponseGRPC(serviceResponse));
            responseObserver.onCompleted();
        } catch (TransactionNotFoundException e) {
            log.warn("Transaction not found for update: {}", request.getTransactionId());
            handleError(responseObserver, Code.NOT_FOUND, e.getMessage());
        } catch (InvalidTransactionStatusException e) {
            log.warn("Invalid status update for transaction: {}", request.getTransactionId());
            handleError(responseObserver, Code.INVALID_ARGUMENT, e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid argument for transaction update: {}", e.getMessage(), e);
            handleError(responseObserver, Code.INVALID_ARGUMENT, "Invalid transaction ID or status format.");
        } catch (Exception e) {
            log.error("Error updating transaction status: {}", e.getMessage(), e);
            handleError(responseObserver, Code.INTERNAL, "An unexpected error occurred while updating transaction status.");
        }
    }

    @Override
    public void getTransactionStatus(TransactionIdRequestGRPC request, StreamObserver<StatusResponseGRPC> responseObserver) {
        try {
            // Get status using DTO
            StatusResponseDto serviceResponse = transactionService.getTransactionStatus(
                java.util.UUID.fromString(request.getTransactionId())
            );

            // Build gRPC response
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
            log.error("Error getting transaction status: {}", e.getMessage(), e);
            handleError(responseObserver, Code.INTERNAL, "An unexpected error occurred while fetching transaction status.");
        }
    }

    @Override
    public void verifyTransactionOtp(VerifyTransactionOtpRequestGRPC request,
                                     StreamObserver<TransactionResponseGRPC> responseObserver) {
        try {
            // 1. Parse transaction ID
            UUID transactionId = UUID.fromString(request.getTransactionId());
            String otpCode = request.getOtpCode();

            log.info("GRPC: Verifying OTP for transaction ID: {}", transactionId);

            // 2. Call the service layer
            TransactionResponseDto responseDto = transactionService.verifyTransactionOtp(transactionId, otpCode);

            // 3. Convert and send response
            responseObserver.onNext(convertToTransactionResponseGRPC(responseDto));
            responseObserver.onCompleted();

        } catch (IllegalArgumentException e) {
            log.warn("Invalid transaction ID format: {}", request.getTransactionId(), e);
            handleError(responseObserver, Code.INVALID_ARGUMENT, "Invalid transaction ID format");
        } catch (TransactionNotFoundException e) {
            log.warn("Transaction not found: {}", request.getTransactionId(), e);
            handleError(responseObserver, Code.NOT_FOUND, e.getMessage());
        } catch (TransactionProcessingException e) {
            log.warn("OTP verification failed for transaction: {}", request.getTransactionId(), e);
            handleError(responseObserver, Code.FAILED_PRECONDITION, e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during OTP verification for transaction: {}",
                    request.getTransactionId(), e);
            handleError(responseObserver, Code.INTERNAL,
                    "An unexpected error occurred during OTP verification");
        }
    }


    // Helper methods
    private OverallStatusGRPC convertToOverallStatusGRPC(org.openapitools.model.OverallStatusEnum statusEnum) {
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

    private TransactionResponseGRPC convertToTransactionResponseGRPC(TransactionResponseDto dto) {
        if (dto == null) {
            return TransactionResponseGRPC.getDefaultInstance();
        }

        TransactionResponseGRPC.Builder builder = TransactionResponseGRPC.newBuilder();
        if (dto.getTransactionId() != null) builder.setTransactionId(dto.getTransactionId().toString());
        if (dto.getSenderId() != null) builder.setSenderId(dto.getSenderId());
        if (dto.getReceiverId() != null) builder.setReceiverId(dto.getReceiverId());
        if (dto.getAmount() != null) builder.setAmount(dto.getAmount());
        if (dto.getDescription() != null) builder.setDescription(dto.getDescription());
        if (dto.getStatus() != null) builder.setStatus(convertToOverallStatusGRPC(dto.getStatus()));
        if (dto.getCreatedAt() != null) builder.setCreatedAt(convertToTimestamp(dto.getCreatedAt()));
        if (dto.getUpdatedAt() != null) builder.setUpdatedAt(convertToTimestamp(dto.getUpdatedAt()));

        return builder.build();
    }

    // Pagination conversion is now handled inline in getUserTransactions

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
