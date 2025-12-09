package com.central.transaction_service.dto;

import org.openapitools.model.OverallStatusEnum;

import java.util.UUID;

public interface StatusResponseDto {
    UUID getTransactionId();
    OverallStatusEnum getStatus();
}
