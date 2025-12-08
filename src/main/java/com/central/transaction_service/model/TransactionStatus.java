package com.central.transaction_service.model;

import java.util.Arrays;
import java.util.List;

/**
 * Represents the status of a transaction in the system.
 * Each status indicates a specific state in the transaction lifecycle.
 */
public enum TransactionStatus {
    // Initial states
    PENDING("Pending", "Transaction has been created but not yet processed"),
    PROCESSING("Processing", "Transaction is being processed"),
    
    // Success states
    COMPLETED("Completed", "Transaction was successfully completed"),
    REFUNDED("Refunded", "Transaction was successfully refunded"),
    
    // Intermediate states
    REFUND_INITIATED("Refund Initiated", "Refund process has been initiated"),
    
    // Final failure states
    FAILED("Failed", "Transaction failed to process"),
    DECLINED("Declined", "Transaction was declined by business rules"),
    CANCELLED("Cancelled", "Transaction was cancelled by the user"),
    EXPIRED("Expired", "Transaction expired before completion");

    private final String displayName;
    private final String description;

    TransactionStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    /**
     * Returns a user-friendly display name for the status
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns a description of what this status means
     */
    public String getDescription() {
        return description;
    }

    /**
     * Checks if this status is terminal (no further state changes expected)
     */
    public boolean isTerminal() {
        return List.of(COMPLETED, REFUNDED, FAILED, DECLINED, CANCELLED, EXPIRED)
                .contains(this);
    }

    /**
     * Checks if this status indicates a successful transaction
     */
    public boolean isSuccessful() {
        return this == COMPLETED || this == REFUNDED;
    }

    /**
     * Checks if this status indicates a failed or error state
     */
    public boolean isFailed() {
        return this == FAILED || this == DECLINED || this == EXPIRED;
    }

    /**
     * Checks if this status indicates a pending or processing state
     */
    public boolean isPending() {
        return this == PENDING || this == PROCESSING;
    }

    /**
     * Gets all terminal statuses
     */
    public static List<TransactionStatus> getTerminalStatuses() {
        return Arrays.stream(values())
                .filter(TransactionStatus::isTerminal)
                .toList();
    }

    /**
     * Gets all non-terminal statuses
     */
    public static List<TransactionStatus> getNonTerminalStatuses() {
        return Arrays.stream(values())
                .filter(status -> !status.isTerminal())
                .toList();
    }
}
