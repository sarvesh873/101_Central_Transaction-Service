package com.central.transaction_service.dto;

import com.central.transaction.TransactionRequestGRPC;

public class GrpcTransactionRequestAdapter implements TransactionRequestDto {
    private final TransactionRequestGRPC request;

    public GrpcTransactionRequestAdapter(TransactionRequestGRPC request) {
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
