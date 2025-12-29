package com.central.transaction_service.dto;

public interface TransactionRequestDto {
    String getSenderId();
    String getReceiverId();
    Double getAmount();
    String getDescription();
}
