package com.central.transaction_service.service;

import org.openapitools.model.StatusResponse;
import org.openapitools.model.StatusUpdateRequest;
import org.openapitools.model.TransactionRequest;
import org.openapitools.model.TransactionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public interface TransactionService {

    TransactionResponse createTransaction(TransactionRequest transactionRequest);

    Page<TransactionResponse> getUserTransactions(
            String userCode,
            String status,
            OffsetDateTime fromDate,
            OffsetDateTime toDate,
            Pageable pageable

    );

    TransactionResponse getTransactionDetails(UUID transactionId);

    TransactionResponse updateTransactionStatus(UUID transactionId, StatusUpdateRequest statusUpdateRequest);

    StatusResponse getTransactionStatus(UUID transactionId);

}
