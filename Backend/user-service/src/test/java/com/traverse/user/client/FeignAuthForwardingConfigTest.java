package com.traverse.user.client;

import com.traverse.user.logging.CorrelationIdFilter;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;

class FeignAuthForwardingConfigTest {

    private final FeignAuthForwardingConfig config = new FeignAuthForwardingConfig();

    @AfterEach
    void cleanup() {
        RequestContextHolder.resetRequestAttributes();
        MDC.clear();
    }

    @Test
    void authInterceptorForwardsCookieTokenAsBearer() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("access_token", "jwt-abc"));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        RequestInterceptor interceptor = config.authForwardingInterceptor("access_token");
        RequestTemplate template = new RequestTemplate();
        interceptor.apply(template);

        assertThat(template.headers().get("Authorization")).containsExactly("Bearer jwt-abc");
    }

    @Test
    void authInterceptorAddsNothingWhenNoRequestContext() {
        RequestInterceptor interceptor = config.authForwardingInterceptor("access_token");
        RequestTemplate template = new RequestTemplate();
        interceptor.apply(template);

        assertThat(template.headers()).doesNotContainKey("Authorization");
    }

    @Test
    void authInterceptorAddsNothingWhenRequestHasNoCookies() {
        MockHttpServletRequest request = new MockHttpServletRequest(); // no cookies set
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        RequestInterceptor interceptor = config.authForwardingInterceptor("access_token");
        RequestTemplate template = new RequestTemplate();
        interceptor.apply(template);

        assertThat(template.headers()).doesNotContainKey("Authorization");
    }

    @Test
    void correlationInterceptorForwardsMdcValue() {
        MDC.put(CorrelationIdFilter.MDC_KEY, "cid-xyz");

        RequestInterceptor interceptor = config.correlationIdForwardingInterceptor();
        RequestTemplate template = new RequestTemplate();
        interceptor.apply(template);

        assertThat(template.headers().get(CorrelationIdFilter.HEADER)).containsExactly("cid-xyz");
    }

    @Test
    void correlationInterceptorAddsNothingWhenMdcEmpty() {
        RequestInterceptor interceptor = config.correlationIdForwardingInterceptor();
        RequestTemplate template = new RequestTemplate();
        interceptor.apply(template);

        assertThat(template.headers()).doesNotContainKey(CorrelationIdFilter.HEADER);
    }
}
