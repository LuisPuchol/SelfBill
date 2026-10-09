package com.luispuchol.selfbill.selfbill_api.config;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        Instant start = Instant.now();
        String queryString = request.getQueryString();
        String fullPath = queryString != null ? request.getRequestURI() + "?" + queryString : request.getRequestURI();
        log.info("🚀 {} {}", request.getMethod(), fullPath);

        try {
            filterChain.doFilter(request, response);
        } finally {
            int status = response.getStatus();
            long duration = Duration.between(start, Instant.now()).toMillis();
            log.info("{} {} {} - {}ms", statusEmoji(status), status, statusText(status), duration);
        }
    }

    private static String statusEmoji(int status) {
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

    private static String statusText(int status) {
        return switch (status) {
            case 200 -> "OK";
            case 201 -> "CREATED";
            case 204 -> "NO CONTENT";
            case 400 -> "BAD REQUEST";
            case 401 -> "UNAUTHORIZED";
            case 403 -> "FORBIDDEN";
            case 404 -> "NOT FOUND";
            case 409 -> "CONFLICT";
            case 500 -> "INTERNAL SERVER ERROR";
            case 503 -> "SERVICE UNAVAILABLE";
            default -> "";
        };
    }
}
