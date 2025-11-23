package com.central.transaction_service.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


import java.io.IOException;

/**
 * This class is a filter that logs the start and end of a transaction. It logs the HTTP method, URL, and the time
 * taken to complete the transaction.
 */
@Component
@Slf4j
public class LogFilter implements Filter {

    /**
     * This method is called by the servlet container each time a request/response pair is passed
     * through the chain due to a client request for a resource at the end of the chain.
     *
     * @param servletRequest  the request to process
     * @param servletResponse the response associated with the request
     * @param filterChain      the chain of the next filter
     * @throws IOException      if an input/output error occurs
     * @throws ServletException if a servlet error occurs
     */
    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        final HttpServletRequest request = (HttpServletRequest) servletRequest;

        // Log the start of the transaction
        log.info("Transaction Started for : {} {}", request.getMethod(), request.getRequestURL());

        // Get the current time
        long time = System.currentTimeMillis();

        // Call the next filter in the chain
        filterChain.doFilter(servletRequest, servletResponse);

        // Calculate the time taken to complete the transaction
        time = System.currentTimeMillis() - time;

        // Log the end of the transaction
        log.info("Transaction Completed for : {} {} in {} ms", request.getMethod(), request.getRequestURL(), time);
    }
}
