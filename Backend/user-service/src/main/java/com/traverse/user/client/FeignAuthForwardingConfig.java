package com.traverse.user.client;

import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.Optional;

/**
 * User Service has no session/cookie jar of its own -- when it calls Auth
 * Service on an admin's behalf, it forwards that admin's own JWT (read from
 * the incoming request's cookie) as a Bearer header, so Auth Service's
 * role-escalation checks apply to the real caller, not to User Service.
 */
@Configuration
public class FeignAuthForwardingConfig {

    @Bean
    public RequestInterceptor authForwardingInterceptor(@Value("${app.cookie.name}") String cookieName) {
        return template -> currentRequest()
                .flatMap(request -> extractToken(request, cookieName))
                .ifPresent(token -> template.header("Authorization", "Bearer " + token));
    }

    private Optional<HttpServletRequest> currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            return Optional.of(attrs.getRequest());
        }
        return Optional.empty();
    }

    private Optional<String> extractToken(HttpServletRequest request, String cookieName) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }
        return Arrays.stream(request.getCookies())
                .filter(cookie -> cookieName.equals(cookie.getName()))
                .map(jakarta.servlet.http.Cookie::getValue)
                .findFirst();
    }
}
