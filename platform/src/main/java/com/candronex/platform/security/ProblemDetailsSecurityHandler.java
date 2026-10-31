package com.candronex.platform.security;

import com.candronex.platform.web.Problems;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;

/** Réponses 401 et 403 du serveur de ressources. */
class ProblemDetailsSecurityHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private static final Logger log = LoggerFactory.getLogger(ProblemDetailsSecurityHandler.class);

    private final BearerTokenAuthenticationEntryPoint bearerEntryPoint =
            new BearerTokenAuthenticationEntryPoint();
    private final BearerTokenAccessDeniedHandler bearerAccessDenied =
            new BearerTokenAccessDeniedHandler();
    private final ObjectMapper objectMapper;

    ProblemDetailsSecurityHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException exception) throws IOException {
        bearerEntryPoint.commence(request, response, exception);
        write(request, response, HttpStatus.UNAUTHORIZED, "unauthorized", "Authentification requise",
                "Le jeton est absent, expiré ou invalide.");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException exception) throws IOException {
        bearerAccessDenied.handle(request, response, exception);
        write(request, response, HttpStatus.FORBIDDEN, "forbidden", "Accès refusé",
                "Cet appelant n'est pas autorisé à utiliser cette opération.");
    }

    private void write(HttpServletRequest request, HttpServletResponse response, HttpStatus status,
                       String type, String title, String detail) throws IOException {
        String correlationId = Problems.correlationId();
        log.warn("status={} error={}", status.value(), type);
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(),
                Problems.build(request.getRequestURI(), correlationId, status, type, title, detail));
    }
}
