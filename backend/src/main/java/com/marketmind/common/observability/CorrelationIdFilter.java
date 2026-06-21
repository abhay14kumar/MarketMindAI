package com.marketmind.common.observability;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    public static final String REQUEST_ID_HEADER = "X-Request-ID";
    public static final String CORRELATION_ID_ATTRIBUTE =
            CorrelationIdFilter.class.getName() + ".correlationId";
    public static final String MDC_CORRELATION_ID = "correlationId";
    public static final String MDC_REQUEST_ID = "requestId";

    private static final Logger LOGGER =
            LoggerFactory.getLogger(CorrelationIdFilter.class);
    private static final int MAX_IDENTIFIER_LENGTH = 128;
    private static final Pattern SAFE_IDENTIFIER =
            Pattern.compile("[A-Za-z0-9][A-Za-z0-9._:-]{0,127}");

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String correlationId = resolveCorrelationId(request);
        long startedAt = System.nanoTime();

        request.setAttribute(CORRELATION_ID_ATTRIBUTE, correlationId);
        response.setHeader(CORRELATION_ID_HEADER, correlationId);
        response.setHeader(REQUEST_ID_HEADER, correlationId);
        MDC.put(MDC_CORRELATION_ID, correlationId);
        MDC.put(MDC_REQUEST_ID, correlationId);

        try {
            LOGGER.atInfo()
                    .addKeyValue("httpMethod", request.getMethod())
                    .addKeyValue("httpPath", request.getRequestURI())
                    .log("HTTP request started");
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = (System.nanoTime() - startedAt) / 1_000_000;
            LOGGER.atInfo()
                    .addKeyValue("httpMethod", request.getMethod())
                    .addKeyValue("httpPath", request.getRequestURI())
                    .addKeyValue("httpStatus", response.getStatus())
                    .addKeyValue("durationMs", durationMs)
                    .log("HTTP request completed");
            MDC.remove(MDC_CORRELATION_ID);
            MDC.remove(MDC_REQUEST_ID);
        }
    }

    private String resolveCorrelationId(HttpServletRequest request) {
        String correlationId = firstSafe(
                request.getHeader(CORRELATION_ID_HEADER),
                request.getHeader(REQUEST_ID_HEADER));
        return correlationId == null ? UUID.randomUUID().toString() : correlationId;
    }

    private String firstSafe(String... candidates) {
        for (String candidate : candidates) {
            if (candidate != null) {
                String normalized = candidate.strip();
                if (normalized.length() <= MAX_IDENTIFIER_LENGTH
                        && SAFE_IDENTIFIER.matcher(normalized).matches()) {
                    return normalized;
                }
            }
        }
        return null;
    }
}
