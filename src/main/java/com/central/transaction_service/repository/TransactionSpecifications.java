package com.central.transaction_service.repository;

import com.central.transaction_service.model.Transaction;
import com.central.transaction_service.model.TransactionStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

public class TransactionSpecifications {

    public static Specification<Transaction> hasUserCode(String userCode) {
        return (root, query, criteriaBuilder) -> 
            criteriaBuilder.or(
                criteriaBuilder.equal(root.get("senderId"), userCode),
                criteriaBuilder.equal(root.get("receiverId"), userCode)
            );
    }

    public static Specification<Transaction> hasStatus(TransactionStatus status) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("status"), status);
    }

    public static Specification<Transaction> createdAfter(LocalDateTime date) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.greaterThanOrEqualTo(root.get("initiatedAt"), date);
    }

    public static Specification<Transaction> createdBefore(LocalDateTime date) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.lessThanOrEqualTo(root.get("initiatedAt"), date);
    }
}
