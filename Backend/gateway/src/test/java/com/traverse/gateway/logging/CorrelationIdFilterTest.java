package com.traverse.gateway.logging;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    void generatesCorrelationIdWhenAbsentAndForwardsAndEchoesIt() {
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/users/1").build());
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());
        ArgumentCaptor<ServerWebExchange> forwarded = ArgumentCaptor.forClass(ServerWebExchange.class);

        filter.filter(exchange, chain).block();

        verify(chain).filter(forwarded.capture());
        String cid = forwarded.getValue().getRequest().getHeaders().getFirst(CorrelationIdFilter.HEADER);
        assertThat(cid).isNotBlank();
        // doFinally echoes the same id back on the (original) response
        assertThat(exchange.getResponse().getHeaders().getFirst(CorrelationIdFilter.HEADER)).isEqualTo(cid);
    }

    @Test
    void reusesIncomingCorrelationId() {
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/users/1")
                        .header(CorrelationIdFilter.HEADER, "existing-id-123").build());
        GatewayFilterChain chain = mock(GatewayFilterChain.class);
        when(chain.filter(any())).thenReturn(Mono.empty());
        ArgumentCaptor<ServerWebExchange> forwarded = ArgumentCaptor.forClass(ServerWebExchange.class);

        filter.filter(exchange, chain).block();

        verify(chain).filter(forwarded.capture());
        assertThat(forwarded.getValue().getRequest().getHeaders().getFirst(CorrelationIdFilter.HEADER))
                .isEqualTo("existing-id-123");
    }

    @Test
    void runsAtHighestPrecedence() {
        assertThat(filter.getOrder()).isEqualTo(Ordered.HIGHEST_PRECEDENCE);
    }
}
