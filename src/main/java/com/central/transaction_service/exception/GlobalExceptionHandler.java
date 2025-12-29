package com.central.transaction_service.exception;

import org.openapitools.model.ErrorResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import io.grpc.Status;
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

    @ExceptionHandler(GrpcServiceException.class)
    public ResponseEntity<ErrorResponse> handleGrpcServiceException(GrpcServiceException ex) {
        Double errorCode = 504.01; /*Wallet service errors*/
        String description = ex.getLocalizedMessage();
        HttpStatus httpStatus = convertStatus(ex.getStatus());
        String errorType = httpStatus.getReasonPhrase();
        String errorMessage = ex.getMessage();
        return generateErrorResponse(errorCode, description, errorType, errorMessage, httpStatus);
    }

    private HttpStatus convertStatus(Status status) {
        return switch (status.getCode()) {
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case INVALID_ARGUMENT -> HttpStatus.BAD_REQUEST;
            case PERMISSION_DENIED -> HttpStatus.FORBIDDEN;
            case UNAUTHENTICATED -> HttpStatus.UNAUTHORIZED;
            case FAILED_PRECONDITION -> HttpStatus.PRECONDITION_FAILED;
            case ALREADY_EXISTS -> HttpStatus.CONFLICT;
            case RESOURCE_EXHAUSTED -> HttpStatus.TOO_MANY_REQUESTS;
            case UNAVAILABLE -> HttpStatus.SERVICE_UNAVAILABLE;
            case DEADLINE_EXCEEDED -> HttpStatus.REQUEST_TIMEOUT;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
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
