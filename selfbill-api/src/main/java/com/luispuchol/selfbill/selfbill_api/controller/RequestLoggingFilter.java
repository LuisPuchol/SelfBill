package com.luispuchol.selfbill.selfbill_api.controller;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.micrometer.common.lang.NonNull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String method = request.getMethod();
        String uri = request.getRequestURI();

        log.info("{} {}", method, uri);

        filterChain.doFilter(request, response);

        int status = response.getStatus();
        String statusText = getStatusText(status);
        log.info("Completed {} {}", status, statusText);
    }

    private String getStatusText(int status) {
        return switch (status) {
            case 200 -> "OK";
            case 201 -> "CREATED";
            case 204 -> "NO CONTENT";
            case 400 -> "BAD REQUEST";
            case 404 -> "NOT FOUND";
            case 500 -> "INTERNAL SERVER ERROR";
            default -> "";
        };
    }
}
