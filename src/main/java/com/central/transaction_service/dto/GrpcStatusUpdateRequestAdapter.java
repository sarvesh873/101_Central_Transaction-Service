package com.central.transaction_service.dto;

import com.central.transaction.UpdateTransactionRequestGRPC;
import org.openapitools.model.OverallStatusEnum;

public class GrpcStatusUpdateRequestAdapter implements StatusUpdateRequestDto {
    private final UpdateTransactionRequestGRPC request;

    public GrpcStatusUpdateRequestAdapter(UpdateTransactionRequestGRPC request) {
        this.request = request;
    }

    @Override
    public OverallStatusEnum getStatus() {
        return OverallStatusEnum.valueOf(request.getUpdateData().getStatus().name());
    }
}
