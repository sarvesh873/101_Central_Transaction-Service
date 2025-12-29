package com.central.transaction_service.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Configuration class for managing thread pool executors in the application.
 * This class configures two types of thread pools:
 * 1. IO-bound task executor using virtual threads (Java 19+)
 * 2. CPU-bound task executor using platform threads
 * 
 * The configuration is designed to optimize performance for different types of workloads.
 */
@Configuration
@EnableAsync
public class ThreadPoolConfig {

    /**
     * Number of threads in the CPU-bound thread pool.
     * Defaults to the number of available processors.
     */
    @Value("${threading.cpu.pool-size:#{T(java.lang.Runtime).getRuntime().availableProcessors()}}")
    private int cpuPoolSize;

    /**
     * Thread name prefix for CPU-bound threads.
     * Useful for debugging and monitoring purposes.
     */
    @Value("${threading.cpu.name-prefix:cpu-pf-}")
    private String cpuNamePrefix;

    // Executor services for different types of tasks
    private ExecutorService ioExecutorService;
    private ExecutorService cpuExecutorService;

    /**
     * Creates and configures an ExecutorService for IO-bound operations.
     * This executor uses virtual threads (Project Loom) which are lightweight
     * and ideal for tasks that spend most of their time waiting (e.g., I/O operations).
     *
     * @return Configured ExecutorService for IO-bound tasks
     */
    @Bean(name = "ioTaskExecutor")
    public ExecutorService ioTaskExecutor() {
        ioExecutorService = Executors.newVirtualThreadPerTaskExecutor();
        return ioExecutorService;
    }

    /**
     * Creates and configures an ExecutorService for CPU-bound operations.
     * This executor uses platform threads and is sized based on the number of available processors.
     * Threads are named with the configured prefix for better debugging.
     *
     * @return Configured ExecutorService for CPU-bound tasks
     */
    @Bean(name = "cpuTaskExecutor")
    public ExecutorService cpuTaskExecutor() {
        // Create a thread factory with the configured naming pattern
        ThreadFactory pf = Thread.ofPlatform().name(cpuNamePrefix, 0).factory();
        // Ensure we always have at least 1 thread in the pool
        cpuExecutorService = Executors.newFixedThreadPool(Math.max(1, cpuPoolSize), pf);
        return cpuExecutorService;
    }

    /**
     * Cleanup method that shuts down all executor services when the application context is closed.
     * This ensures that all threads are properly terminated, preventing resource leaks.
     */
    @PreDestroy
    public void shutdown() {
        if (ioExecutorService != null) {
            ioExecutorService.shutdownNow();
        }
        if (cpuExecutorService != null) {
            cpuExecutorService.shutdownNow();
        }
    }
}
