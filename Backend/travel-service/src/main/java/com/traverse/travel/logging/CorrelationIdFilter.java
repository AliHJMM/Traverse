package com.traverse.travel.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Reads the X-Correlation-Id header set by the Gateway (or generates one if
 * this service is reached directly) and puts it in MDC so every log line
 * for this request carries it -- see logging.pattern.console in
 * application.yml. Echoed back on the response too so callers can
 * correlate their own logs against this service's.
 *
 * Also logs its own access-log-style line per request (like the Gateway's
 * CorrelationIdFilter does) -- a plain successful CRUD request often
 * doesn't hit any other log statement in this service, so without this
 * there'd be nothing here to find in Loki for a correlation ID that only
 * shows up in the Gateway's own log line. Confirmed live during Phase 10
 * verification: the first version of this filter (MDC only, no logging)
 * left every successful request completely untraceable past the Gateway.
 */
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Correlation-Id";
    public static final String MDC_KEY = "correlationId";

    private static final Logger log = LoggerFactory.getLogger(CorrelationIdFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String correlationId = request.getHeader(HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        MDC.put(MDC_KEY, correlationId);
        response.setHeader(HEADER, correlationId);
        long start = System.currentTimeMillis();
        try {
            filterChain.doFilter(request, response);
        } finally {
            log.info("method={} path={} status={} durationMs={}",
                    request.getMethod(), request.getRequestURI(), response.getStatus(),
                    System.currentTimeMillis() - start);
            MDC.remove(MDC_KEY);
        }
    }
}
