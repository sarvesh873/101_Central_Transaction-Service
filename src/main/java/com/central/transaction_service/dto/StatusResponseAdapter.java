package com.central.transaction_service.dto;

import com.central.transaction_service.model.Transaction;
import org.openapitools.model.OverallStatusEnum;

import java.util.UUID;

public class StatusResponseAdapter implements StatusResponseDto {
    private final Transaction transaction;

    public StatusResponseAdapter(Transaction transaction) {
        this.transaction = transaction;
    }

    @Override
    public UUID getTransactionId() {
        return transaction.getTransaction_id();
    }

    @Override
    public OverallStatusEnum getStatus() {
        return OverallStatusEnum.fromValue(transaction.getStatus().name());
    }
}
