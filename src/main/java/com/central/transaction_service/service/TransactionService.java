package com.central.transaction_service.service;

import org.openapitools.model.StatusResponse;
import org.openapitools.model.StatusUpdateRequest;
import org.openapitools.model.TransactionRequest;
import org.openapitools.model.TransactionResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public interface TransactionService {

    ResponseEntity<TransactionResponse> createTransaction(TransactionRequest transactionRequest);

    ResponseEntity<List<TransactionResponse>> getUserTransactions(String userCode);

    ResponseEntity<TransactionResponse> getTransactionDetails(UUID transactionId);

    ResponseEntity<TransactionResponse> updateTransactionStatus(UUID transactionId, StatusUpdateRequest statusUpdateRequest);

    ResponseEntity<StatusResponse> getTransactionStatus(UUID transactionId);

}
