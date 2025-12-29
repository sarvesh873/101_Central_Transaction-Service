package com.central.transaction_service.exception;

import io.grpc.Status;
import lombok.Getter;

@Getter
public class GrpcServiceException extends RuntimeException {
    private final Status status;

    public GrpcServiceException(Status status, String message) {
        super(message);
        this.status = status;
    }

    public GrpcServiceException(Status status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

}
