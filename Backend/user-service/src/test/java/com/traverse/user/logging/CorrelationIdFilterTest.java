package com.traverse.user.logging;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    void generatesCorrelationIdWhenAbsentAndEchoesItOnTheResponse() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/users");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        assertThat(res.getHeader(CorrelationIdFilter.HEADER)).isNotBlank();
        verify(chain).doFilter(req, res);
    }

    @Test
    void reusesIncomingCorrelationId() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/users");
        req.addHeader(CorrelationIdFilter.HEADER, "existing-id-123");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        assertThat(res.getHeader(CorrelationIdFilter.HEADER)).isEqualTo("existing-id-123");
        verify(chain).doFilter(req, res);
    }
}
