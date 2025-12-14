package com.central.transaction_service.service;

import com.central.transaction_service.model.Transaction;
import java.util.concurrent.CompletableFuture;

public interface RefundService {
    CompletableFuture<Void> initiateRefundAsync(Transaction transaction);
}
