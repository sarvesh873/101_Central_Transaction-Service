package com.central.transaction_service.dto;

import com.central.transaction_service.model.Transaction;
import org.openapitools.model.OverallStatusEnum;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;

public class TransactionResponseAdapter implements TransactionResponseDto {
    private final Transaction transaction;

    public TransactionResponseAdapter(Transaction transaction) {
        this.transaction = transaction;
    }

    @Override
    public UUID getTransactionId() {
        return transaction.getTransaction_id();
    }

    @Override
    public String getSenderId() {
        return transaction.getSenderId();
    }

    @Override
    public String getReceiverId() {
        return transaction.getReceiverId();
    }

    @Override
    public Double getAmount() {
        return transaction.getAmount();
    }

    @Override
    public String getDescription() {
        return transaction.getDescription();
    }

    @Override
    public OverallStatusEnum getStatus() {
        return OverallStatusEnum.fromValue(transaction.getStatus().name());
    }

    @Override
    public OffsetDateTime getCreatedAt() {
        return transaction.getInitiatedAt().atZone(ZoneId.systemDefault()).toOffsetDateTime();
    }

    @Override
    public OffsetDateTime getUpdatedAt() {
        return transaction.getUpdatedAt().atZone(ZoneId.systemDefault()).toOffsetDateTime();
    }
}
