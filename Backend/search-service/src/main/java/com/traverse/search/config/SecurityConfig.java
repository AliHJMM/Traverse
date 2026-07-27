package com.traverse.search.config;

import com.traverse.search.logging.CorrelationIdFilter;
import com.traverse.search.security.JwtCookieAuthenticationFilter;
import com.traverse.search.security.JwtService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtService jwtService,
                                                     @Value("${app.cookie.name}") String cookieName) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health").permitAll()
                        // Search + autocomplete are open to any authenticated role
                        // (travelers, managers, admins all search travels).
                        .requestMatchers("/api/search/travels", "/api/search/autocomplete").authenticated()
                        // Internal indexing endpoints -- only ever invoked
                        // service-to-service by travel-service while forwarding a
                        // manager/admin's cookie (travel writes are themselves
                        // manager/admin-only). Gating to those roles stops an
                        // ordinary traveler from tampering with the search index
                        // directly through the gateway.
                        .requestMatchers("/api/search/index/**").hasAnyRole("ADMIN", "TRAVEL_MANAGER")
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex.authenticationEntryPoint(
                        (request, response, authException) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED)))
                .addFilterBefore(new JwtCookieAuthenticationFilter(jwtService, cookieName),
                        UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(new CorrelationIdFilter(), JwtCookieAuthenticationFilter.class);

        return http.build();
    }
}
