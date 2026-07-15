package com.traverse.auth.security;

import com.traverse.auth.entity.Role;
import com.traverse.auth.service.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class JwtCookieAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final String cookieName;

    public JwtCookieAuthenticationFilter(JwtService jwtService, String cookieName) {
        this.jwtService = jwtService;
        this.cookieName = cookieName;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        extractToken(request).ifPresent(token -> {
            try {
                Claims claims = jwtService.parseClaims(token);
                AuthenticatedUser principal = new AuthenticatedUser(
                        Long.valueOf(claims.getSubject()),
                        claims.get("email", String.class),
                        Role.valueOf(claims.get("roles", String.class)));

                var authentication = new UsernamePasswordAuthenticationToken(
                        principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + principal.role())));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (JwtException | IllegalArgumentException e) {
                // Invalid/expired token: leave the request unauthenticated and let
                // the endpoint's own authorization rule (permitAll vs authenticated)
                // decide what happens next, rather than failing the filter chain here.
                SecurityContextHolder.clearContext();
            }
        });
        filterChain.doFilter(request, response);
    }

    private Optional<String> extractToken(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }
        return Arrays.stream(request.getCookies())
                .filter(cookie -> cookieName.equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst();
    }
}
