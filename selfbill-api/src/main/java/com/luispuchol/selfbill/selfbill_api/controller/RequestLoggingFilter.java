package com.luispuchol.selfbill.selfbill_api.controller;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

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

        Instant start = Instant.now();
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String queryString = request.getQueryString();

        String fullPath = queryString != null ? uri + "?" + queryString : uri;
        log.info("🚀 {} {}", method, fullPath);

        try {
            filterChain.doFilter(request, response);
        } finally {
            Instant end = Instant.now();
            long duration = Duration.between(start, end).toMillis();
            int status = response.getStatus();
            String statusEmoji = getStatusEmoji(status);
            String statusText = getStatusText(status);

            log.info("{} {} {} - {}ms", statusEmoji, status, statusText, duration);
        }
    }

    private String getStatusEmoji(int status) {
        if (status >= 200 && status < 300)
            return "✅";
        if (status >= 300 && status < 400)
            return "🔀";
        if (status >= 400 && status < 500)
            return "⚠️";
        if (status >= 500)
            return "❌";
        return "ℹ️";
    }

    private String getStatusText(int status) {
        return switch (status) {
            case 200 -> "OK";
            case 201 -> "CREATED";
            case 204 -> "NO CONTENT";
            case 400 -> "BAD REQUEST";
            case 401 -> "UNAUTHORIZED";
            case 403 -> "FORBIDDEN";
            case 404 -> "NOT FOUND";
            case 500 -> "INTERNAL SERVER ERROR";
            case 503 -> "SERVICE UNAVAILABLE";
            default -> "";
        };
    }
}