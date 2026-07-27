package com.traverse.payment.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    /**
     * Load-balanced so {@code http://travel-service} resolves through Eureka +
     * Spring Cloud LoadBalancer -- used to fetch the authoritative travel price
     * when charging a booking (never trust a client-supplied amount).
     */
    @Bean
    @LoadBalanced
    public RestClient.Builder loadBalancedRestClientBuilder() {
        return RestClient.builder();
    }
}
