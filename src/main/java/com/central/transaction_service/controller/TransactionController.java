package com.central.transaction_service.controller;

import com.central.transaction_service.service.TransactionService;
import lombok.extern.slf4j.Slf4j;
import org.openapitools.model.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.openapitools.api.TransactionsApi;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
public class TransactionController implements TransactionsApi{

    @Autowired
    private TransactionService transactionService;


    @Override
    public ResponseEntity<TransactionResponse> createTransaction(TransactionRequest transactionRequest) {
        return transactionService.createTransaction(transactionRequest);
    }

    @Override
    public ResponseEntity<List<TransactionResponse>> getUserTransactions(String userCode) {
        return transactionService.getUserTransactions(userCode);
    }

    @Override
    public ResponseEntity<TransactionResponse> getTransactionDetails(UUID transactionId) {
        return transactionService.getTransactionDetails(transactionId);
    }

    @Override
    public ResponseEntity<TransactionResponse> updateTransactionStatus(UUID transactionId, StatusUpdateRequest statusUpdateRequest) {
        return transactionService.updateTransactionStatus(transactionId, statusUpdateRequest);
    }

    @Override
    public ResponseEntity<StatusResponse> getTransactionStatus(UUID transactionId) {
        return transactionService.getTransactionStatus(transactionId);
    }

}
