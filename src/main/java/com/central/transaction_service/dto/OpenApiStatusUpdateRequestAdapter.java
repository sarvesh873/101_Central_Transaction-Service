package com.central.transaction_service.dto;

import org.openapitools.model.OverallStatusEnum;
import org.openapitools.model.StatusUpdateRequest;

public class OpenApiStatusUpdateRequestAdapter implements StatusUpdateRequestDto {
    private final StatusUpdateRequest request;

    public OpenApiStatusUpdateRequestAdapter(StatusUpdateRequest request) {
        this.request = request;
    }

    @Override
    public OverallStatusEnum getStatus() {
        return request.getStatus();
    }
}
