package com.candronex.slice.infrastructure.security;

import com.candronex.platform.security.BearerSecurity;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;

/** Routes internes réservées à Order Service : tout autre appelant reçoit 403. */
@Configuration
public class SecurityConfiguration {

    static final String ORDER_SERVICE = "order-service";

    @Bean
    SecurityFilterChain apiSecurity(HttpSecurity http, ObjectMapper objectMapper) throws Exception {
        return BearerSecurity.build(http, objectMapper, auth -> auth
                .requestMatchers("/internal/v1/service-activations").access(orderServiceOnly()));
    }

    private static AuthorizationManager<RequestAuthorizationContext> orderServiceOnly() {
        return (authentication, context) -> new AuthorizationDecision(
                authentication.get() instanceof JwtAuthenticationToken jwt
                        && ORDER_SERVICE.equals(jwt.getToken().getClaimAsString("azp")));
    }
}
