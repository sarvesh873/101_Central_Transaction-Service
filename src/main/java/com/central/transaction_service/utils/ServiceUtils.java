package com.central.transaction_service.utils;

import com.central.transaction_service.model.Transaction;
import org.openapitools.model.TransactionResponse;
import org.openapitools.model.OverallStatusEnum;
import java.time.ZoneId;

public final class ServiceUtils {

    // Private constructor to prevent instantiation
    private ServiceUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }


    public static TransactionResponse constructTransactionResponse(Transaction transaction){
        return TransactionResponse.builder()
                .transactionId(transaction.getTransaction_id())
                .senderId(transaction.getSenderId())
                .receiverId(transaction.getReceiverId())
                .amount(transaction.getAmount())
                .description(transaction.getDescription())
                .status(OverallStatusEnum.fromValue(transaction.getStatus().name()))
                .createdAt(transaction.getInitiatedAt().atZone(ZoneId.systemDefault()).toOffsetDateTime())
                .updatedAt(transaction.getUpdatedAt().atZone(ZoneId.systemDefault()).toOffsetDateTime())
                .build();
    }

}
