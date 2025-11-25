package com.central.transaction_service.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "central_transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @Column(name = "transaction_id", columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID transaction_id;

    // --- Transaction Details ---

    @Column(name = "sender_id", nullable = false)
    private String senderId;

    @Column(name = "receiver_id", nullable = false)
    private String receiverId;

    @Column(name = "amount", nullable = false)
    @Positive(message = "Amount must be positive")
    private Double amount;

    @Column(name = "description", nullable = true)
    private String description;

    // --- Status and Timestamps ---

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    // Note: JPA will handle the mapping from snake_case in DB to camelCase here
    @Column(name = "initiated_at", nullable = false)
    private LocalDateTime initiatedAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;


    @PrePersist
    public void prePersist() {
        this.transaction_id = UUID.randomUUID();
        this.initiatedAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if(status == null){
            status = "PENDING";
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

}
