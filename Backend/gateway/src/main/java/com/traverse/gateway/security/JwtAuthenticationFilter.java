package com.traverse.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/auth/login",
            "/api/auth/register",
            "/actuator/health"
    );

    private final SecretKey signingKey;
    private final String cookieName;

    public JwtAuthenticationFilter(@Value("${jwt.secret}") String secret,
                                    @Value("${app.cookie.name}") String cookieName) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.cookieName = cookieName;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        if (PUBLIC_PATHS.stream().anyMatch(path::startsWith)) {
            return chain.filter(exchange);
        }

        String token = extractToken(request);
        if (token == null) {
            return reject(exchange, "Missing auth token");
        }

        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-User-Id", claims.getSubject())
                    .header("X-User-Roles", String.valueOf(claims.get("roles")))
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        } catch (ExpiredJwtException e) {
            return reject(exchange, "Token expired");
        } catch (JwtException | IllegalArgumentException e) {
            return reject(exchange, "Invalid token");
        }
    }

    /**
     * The JWT is normally carried in an httpOnly cookie (set by the Auth
     * Service) so browser-side JS can never read it, which is the point of
     * using a cookie instead of localStorage. The Authorization header is
     * kept as a fallback for non-browser clients (curl/Postman/service calls).
     */
    private String extractToken(ServerHttpRequest request) {
        HttpCookie cookie = request.getCookies().getFirst(cookieName);
        if (cookie != null) {
            return cookie.getValue();
        }

        String header = request.getHeaders().getFirst("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring("Bearer ".length());
        }

        return null;
    }

    private Mono<Void> reject(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("Content-Type", "application/json");
        byte[] bytes = ("{\"error\":\"" + message + "\"}").getBytes(StandardCharsets.UTF_8);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
