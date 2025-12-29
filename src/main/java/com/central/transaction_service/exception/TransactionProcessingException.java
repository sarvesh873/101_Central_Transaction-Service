package com.central.transaction_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class TransactionProcessingException extends RuntimeException {
    public TransactionProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
    public TransactionProcessingException(String message) {
        super(message);
    }
}
