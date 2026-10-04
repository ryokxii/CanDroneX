package com.candronex.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Serveur de ressources OAuth 2.0 : chaque appel à /api/** doit porter un jeton Bearer JWT valide,
 * et la portée propre à l'opération.
 */
@Configuration
public class SecurityConfiguration {

    private static final String SCOPE = "SCOPE_";

    @Bean
    SecurityFilterChain apiSecurity(HttpSecurity http, ObjectMapper objectMapper) throws Exception {
        ProblemDetailsSecurityHandler problems = new ProblemDetailsSecurityHandler(objectMapper);

        http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/oauth2/token").permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/health").permitAll()
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/drones", "/api/v1/drones/**")
                                .hasAuthority(SCOPE + ApiScopes.DRONES_READ)
                        .requestMatchers(HttpMethod.POST, "/api/v1/drones")
                                .hasAuthority(SCOPE + ApiScopes.DRONES_WRITE)
                        .requestMatchers(HttpMethod.GET, "/api/v1/service-orders/**")
                                .hasAuthority(SCOPE + ApiScopes.ORDERS_READ)
                        .requestMatchers(HttpMethod.POST, "/api/v1/service-orders")
                                .hasAuthority(SCOPE + ApiScopes.ORDERS_WRITE)
                        // Toute route non listée est refusée.
                        .anyRequest().denyAll())
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .jwt(Customizer.withDefaults())
                        .authenticationEntryPoint(problems)
                        .accessDeniedHandler(problems))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(problems)
                        .accessDeniedHandler(problems));

        return http.build();
    }

    /** Empreinte des secrets clients : BCrypt, salé, à coût ajustable. */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
