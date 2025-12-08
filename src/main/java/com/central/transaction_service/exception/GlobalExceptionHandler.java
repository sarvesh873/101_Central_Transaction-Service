package com.central.transaction_service.exception;

import org.openapitools.model.ErrorResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
/**
 * Global exception handler for the application.
 * Centralizes exception handling across all @Controller components.
 * Converts exceptions into appropriate HTTP responses with standardized error formats.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles validation errors and invalid input parameters.
     *
     * @param ex the caught IllegalArgumentException
     * @return ResponseEntity with HTTP 400 Bad Request status and error message
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(IllegalArgumentException ex) {
        Double errorCode = 400.01;
        String description = "Invalid Input please check the input parameters";
        String errorType = HttpStatus.BAD_REQUEST.getReasonPhrase();
        String errorMessage = ex.getMessage();
        return generateErrorResponse(errorCode, description, errorType, errorMessage, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(TransactionNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTransactionNotFoundException(TransactionNotFoundException ex) {
        return generateErrorResponse(404.01, "Transaction not found", "NOT_FOUND", ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(TransactionProcessingException.class)
    public ResponseEntity<ErrorResponse> handleTransactionProcessingException(TransactionProcessingException ex) {
        return generateErrorResponse(500.01, "Transaction processing failed", "INTERNAL_SERVER_ERROR", ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(InvalidTransactionStatusException.class)
    public ResponseEntity<ErrorResponse> handleInvalidTransactionStatusException(InvalidTransactionStatusException ex) {
        return generateErrorResponse(400.01, "Invalid transaction status", "BAD_REQUEST", ex.getMessage(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleConflict(DataIntegrityViolationException ex) {
        Double errorCode = 400.02;
        String description = "Data integrity violation";
        String errorType = HttpStatus.CONFLICT.getReasonPhrase();
        String errorMessage = ex.getMostSpecificCause().getMessage();
        return generateErrorResponse(errorCode, description, errorType, errorMessage, HttpStatus.CONFLICT);
    }

    /**
     * Helper method to generate a standardized error response.
     *
     * @param errorCode the HTTP status code
     * @param description a brief description of the error type
     * @param errorType the HTTP status reason phrase
     * @param errorMessage detailed error message for debugging
     * @return ResponseEntity containing the error details
     */
    private ResponseEntity<ErrorResponse> generateErrorResponse(
            Double errorCode,
            String description,
            String errorType,
            String errorMessage,
            HttpStatus httpStatus) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode(errorCode)
                .description(description)
                .errorType(errorType)
                .errorMessage(errorMessage)
                .build();

        return ResponseEntity.status(httpStatus).body(errorResponse);
    }
}
