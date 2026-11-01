package com.candronex.order.infrastructure.security;

import com.candronex.platform.security.BearerSecurity;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/** Tout client authentifié ; une commande ne vise que ses propres drones. */
@Configuration
public class SecurityConfiguration {

    @Bean
    SecurityFilterChain apiSecurity(HttpSecurity http, ObjectMapper objectMapper) throws Exception {
        return BearerSecurity.build(http, objectMapper, auth -> auth
                .requestMatchers(HttpMethod.POST, "/api/v1/service-orders").authenticated());
    }
}
