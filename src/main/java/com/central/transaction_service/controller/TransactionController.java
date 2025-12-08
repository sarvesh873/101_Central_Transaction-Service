package com.central.transaction_service.controller;

import com.central.transaction_service.service.TransactionService;
import io.micrometer.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.openapitools.model.*;
import org.openapitools.api.TransactionsApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import java.time.OffsetDateTime;
import java.util.UUID;

@Slf4j
@RestController
@RestControllerAdvice
public class TransactionController implements TransactionsApi {

    @Autowired
    private TransactionService transactionService;

    @Override
    public ResponseEntity<TransactionResponse> createTransaction(TransactionRequest transactionRequest) {
        log.info("Creating new transaction");
        TransactionResponse response = transactionService.createTransaction(transactionRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Override
    public ResponseEntity<GetUserTransactions200Response> getUserTransactions(String userCode, String status,
                                                                              OffsetDateTime fromDate, OffsetDateTime toDate,
                                                                              Integer page, Integer pageSize,
                                                                              String sortBy, Boolean sortDesc) {
        log.info("Fetching transactions for user: {}", userCode);
        // Set default values if not provided
        // Spring Pageable is 0-indexed, but the API is 1-indexed.
        // Default to page 1 if not provided, and subtract 1.
        int pageNumber = page != null && page > 0 ? page - 1 : 0;
        int size = pageSize != null ? pageSize : 20;
        String sortField = StringUtils.isNotBlank(sortBy) ? sortBy : "createdAt";
        Sort.Direction direction = Boolean.TRUE.equals(sortDesc) ? Sort.Direction.DESC : Sort.Direction.ASC;

        // Create Pageable for pagination
        Pageable pageable = PageRequest.of(pageNumber, size, Sort.by(direction, sortField));

        // Call service method
        Page<TransactionResponse> holdsPage = transactionService.getUserTransactions(
                userCode,
                status,
                fromDate,
                toDate,
                pageable
        );

        // Debug logging
        log.debug("Total elements: {}, Content size: {}",
                holdsPage.getTotalElements(),
                holdsPage.getContent() != null ? holdsPage.getContent().size() : 0);

        if (holdsPage.getContent() != null && !holdsPage.getContent().isEmpty()) {
            log.debug("First hold in page: {}", holdsPage.getContent().get(0));
        }

        // Create response
        GetUserTransactions200Response response = new GetUserTransactions200Response();
        response.setItems(holdsPage.getContent());
        response.setPagination(new PaginationResponse()
                .currentPage(holdsPage.getNumber() + 1) // Page numbers are 1-based in the response
                .pageSize(holdsPage.getSize())
                .totalItems(holdsPage.getTotalElements())
                .totalPages(holdsPage.getTotalPages())
                .hasNext(holdsPage.hasNext())
                .hasPrevious(holdsPage.hasPrevious())
        );

        log.debug("Response items size: {}", response.getItems() != null ? response.getItems().size() : 0);

        return ResponseEntity.ok(response);
    }


    @Override
    public ResponseEntity<TransactionResponse> getTransactionDetails(UUID transactionId) {
        log.info("Fetching details for transaction ID: {}", transactionId);
        TransactionResponse response = transactionService.getTransactionDetails(transactionId);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<TransactionResponse> updateTransactionStatus(UUID transactionId, StatusUpdateRequest statusUpdateRequest) {
        log.info("Updating status for transaction ID: {}", transactionId);
        TransactionResponse response = transactionService.updateTransactionStatus(transactionId, statusUpdateRequest);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<StatusResponse> getTransactionStatus(UUID transactionId) {
        log.info("Fetching status for transaction ID: {}", transactionId);
        StatusResponse response = transactionService.getTransactionStatus(transactionId);
        return ResponseEntity.ok(response);
    }
}
