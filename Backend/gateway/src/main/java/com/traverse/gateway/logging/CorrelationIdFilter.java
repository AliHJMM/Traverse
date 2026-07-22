package com.traverse.gateway.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * The Gateway is where a correlation ID is born: generates one if the
 * caller didn't send one, stamps it onto the request forwarded to whatever
 * downstream service handles it (and every service-to-service Feign call
 * after that, see FeignAuthForwardingConfig in User Service), and logs an
 * access-log-style line per request so a single request can be traced from
 * here through every service's logs in Loki/Grafana.
 *
 * Runs at HIGHEST_PRECEDENCE, ahead of JwtAuthenticationFilter (order -1),
 * so even rejected/401 requests get a correlation ID and an access log line.
 *
 * MDC isn't used here (unlike the Servlet-based downstream services) --
 * WebFlux's reactive thread-hopping breaks plain MDC without extra Reactor
 * Context plumbing, so the correlation ID is threaded through explicitly as
 * a local variable instead of relying on thread-local state.
 */
@Component
public class CorrelationIdFilter implements GlobalFilter, Ordered {

    public static final String HEADER = "X-Correlation-Id";

    private static final Logger log = LoggerFactory.getLogger(CorrelationIdFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String existing = request.getHeaders().getFirst(HEADER);
        String correlationId = (existing == null || existing.isBlank()) ? UUID.randomUUID().toString() : existing;

        ServerHttpRequest mutatedRequest = request.mutate()
                .header(HEADER, correlationId)
                .build();

        long start = System.currentTimeMillis();
        return chain.filter(exchange.mutate().request(mutatedRequest).build())
                .doFinally(signal -> {
                    // Only set it here if nothing downstream already did --
                    // a proxied route's response already carries the header
                    // (the downstream service's own filter set it on the
                    // same value, since it read it from the request this
                    // filter just stamped), so adding it unconditionally
                    // produced a duplicated comma-joined header value.
                    // Requests the Gateway handles/rejects itself (401s,
                    // no matching route) never reach a downstream service,
                    // so they still need it set here.
                    if (!exchange.getResponse().getHeaders().containsKey(HEADER)) {
                        exchange.getResponse().getHeaders().add(HEADER, correlationId);
                    }
                    log.info("correlationId={} method={} path={} status={} durationMs={}",
                            correlationId,
                            request.getMethod(),
                            request.getURI().getPath(),
                            exchange.getResponse().getStatusCode(),
                            System.currentTimeMillis() - start);
                });
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
