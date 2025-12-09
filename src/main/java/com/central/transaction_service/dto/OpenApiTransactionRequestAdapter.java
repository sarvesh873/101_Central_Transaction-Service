package com.central.transaction_service.dto;

import org.openapitools.model.TransactionRequest;

public class OpenApiTransactionRequestAdapter implements TransactionRequestDto {
    private final TransactionRequest request;

    public OpenApiTransactionRequestAdapter(TransactionRequest request) {
        this.request = request;
    }

    @Override
    public String getSenderId() {
        return request.getSenderId();
    }

    @Override
    public String getReceiverId() {
        return request.getReceiverId();
    }

    @Override
    public Double getAmount() {
        return request.getAmount();
    }

    @Override
    public String getDescription() {
        return request.getDescription();
    }
}
