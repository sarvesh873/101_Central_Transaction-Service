package com.central.transaction_service.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import java.util.concurrent.ExecutorService;

@Slf4j
@Getter
public abstract class BaseGrpcClient {
    protected final ManagedChannel channel;
    protected final ExecutorService ioTaskExecutor;
    protected final String serviceHost;
    protected final int servicePort;

    protected BaseGrpcClient(
            String serviceName,
            String serviceHost,
            int servicePort,
            ExecutorService ioTaskExecutor) {
        
        this.serviceHost = serviceHost;
        this.servicePort = servicePort;
        this.ioTaskExecutor = ioTaskExecutor;
        
        log.info("Initializing Connection to {} Service at {}:{}", serviceName, serviceHost, servicePort);
        
        this.channel = ManagedChannelBuilder.forAddress(serviceHost, servicePort)
                .usePlaintext()
                .executor(ioTaskExecutor)
                .build();
    }

    public void shutdown() {
        try {
            channel.shutdown().awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.warn("Error while shutting down gRPC channel: {}", e.getMessage());
            Thread.currentThread().interrupt();
        }
    }
}
