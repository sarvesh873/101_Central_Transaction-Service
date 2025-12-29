package com.central.transaction_service.dto;

import org.openapitools.model.OverallStatusEnum;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface TransactionResponseDto {
    UUID getTransactionId();
    String getSenderId();
    String getReceiverId();
    Double getAmount();
    String getDescription();
    OverallStatusEnum getStatus();
    OffsetDateTime getCreatedAt();
    OffsetDateTime getUpdatedAt();
}
