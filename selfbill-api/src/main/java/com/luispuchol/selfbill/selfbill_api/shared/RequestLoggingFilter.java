package com.luispuchol.selfbill.selfbill_api.shared;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.springframework.http.HttpStatus;
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
        long start = System.nanoTime();
        String queryString = request.getQueryString();
        String fullPath = queryString != null ? request.getRequestURI() + "?" + queryString : request.getRequestURI();
        log.info("🚀 {} {}", request.getMethod(), fullPath);

        try {
            filterChain.doFilter(request, response);
        } finally {
            int status = response.getStatus();
            long duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            log.info("{} {} {} - {}ms", statusEmoji(status), status, statusText(status), duration);
        }
    }

    private static String statusEmoji(int status) {
        return switch (HttpStatus.Series.resolve(status)) {
            case SUCCESSFUL -> "✅";
            case REDIRECTION -> "🔀";
            case CLIENT_ERROR -> "⚠️";
            case SERVER_ERROR -> "❌";
            case null, default -> "ℹ️";
        };
    }

    private static String statusText(int status) {
        HttpStatus resolved = HttpStatus.resolve(status);
        return resolved != null ? resolved.getReasonPhrase() : "";
    }
}
