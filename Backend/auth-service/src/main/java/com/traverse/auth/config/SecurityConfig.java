package com.traverse.auth.config;

import com.traverse.auth.logging.CorrelationIdFilter;
import com.traverse.auth.security.JwtCookieAuthenticationFilter;
import com.traverse.auth.service.JwtService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtService jwtService,
                                                     @Value("${app.cookie.name}") String cookieName) throws Exception {
        http
                // Stateless JWT-cookie API (no server-side session, no browser form
                // submissions to forge) -- CSRF is mitigated by the cookie's
                // SameSite=Strict attribute instead of a CSRF token.
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/api/auth/register", "/api/auth/login", "/api/auth/logout")
                        .permitAll()
                        .requestMatchers("/api/auth/users/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                // Without this, Spring Security's default entry point returns 403
                // for unauthenticated requests instead of 401.
                .exceptionHandling(ex -> ex.authenticationEntryPoint(
                        (request, response, authException) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED)))
                .addFilterBefore(new JwtCookieAuthenticationFilter(jwtService, cookieName),
                        UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(new CorrelationIdFilter(), JwtCookieAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
