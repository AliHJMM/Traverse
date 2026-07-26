package com.traverse.travel.config;

import com.traverse.travel.logging.CorrelationIdFilter;
import com.traverse.travel.security.JwtCookieAuthenticationFilter;
import com.traverse.travel.security.JwtService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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
                        // Creating/managing travels is for managers & admins;
                        // per-travel ownership (a manager only edits their own)
                        // is enforced in TravelService. Browsing (GET) +
                        // recommendations are open to any authenticated role,
                        // so travelers can search, view, and get suggestions.
                        .requestMatchers(HttpMethod.POST, "/api/travels").hasAnyRole("ADMIN", "TRAVEL_MANAGER")
                        .requestMatchers(HttpMethod.PUT, "/api/travels/**").hasAnyRole("ADMIN", "TRAVEL_MANAGER")
                        .requestMatchers(HttpMethod.DELETE, "/api/travels/**").hasAnyRole("ADMIN", "TRAVEL_MANAGER")
                        .requestMatchers(HttpMethod.GET, "/api/travels/**").authenticated()
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex.authenticationEntryPoint(
                        (request, response, authException) -> response.sendError(HttpServletResponse.SC_UNAUTHORIZED)))
                .addFilterBefore(new JwtCookieAuthenticationFilter(jwtService, cookieName),
                        UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(new CorrelationIdFilter(), JwtCookieAuthenticationFilter.class);

        return http.build();
    }
}
