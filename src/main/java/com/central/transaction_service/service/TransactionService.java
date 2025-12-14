package com.central.transaction_service.service;

import com.central.transaction_service.dto.StatusResponseDto;
import com.central.transaction_service.dto.StatusUpdateRequestDto;
import com.central.transaction_service.dto.TransactionRequestDto;
import com.central.transaction_service.dto.TransactionResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public interface TransactionService {

    TransactionResponseDto createTransaction(TransactionRequestDto transactionRequest);

    Page<TransactionResponseDto> getUserTransactions(
        String userCode,
        String status,
        OffsetDateTime fromDate,
        OffsetDateTime toDate,
        Pageable pageable

    );

    TransactionResponseDto getTransactionDetails(UUID transactionId);

    TransactionResponseDto updateTransactionStatus(UUID transactionId, StatusUpdateRequestDto statusUpdateRequest);

    StatusResponseDto getTransactionStatus(UUID transactionId);

    TransactionResponseDto verifyTransactionOtp(UUID transactionId, String otpCode);

}
