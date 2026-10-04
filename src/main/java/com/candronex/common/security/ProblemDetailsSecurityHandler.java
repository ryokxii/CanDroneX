package com.candronex.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.access.BearerTokenAccessDeniedHandler;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;
import java.net.URI;
import java.util.UUID;

/** Réponses 401 et 403 du serveur de ressources. */
class ProblemDetailsSecurityHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private static final Logger log = LoggerFactory.getLogger(ProblemDetailsSecurityHandler.class);
    private static final String BASE_TYPE = "https://candronex.ca/problems/";

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
        write(response, HttpStatus.UNAUTHORIZED, "unauthenticated", "Authentification requise",
                "Un jeton d'accès Bearer valide est obligatoire. Obtenez-le sur /oauth2/token.");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException exception) throws IOException {
        bearerAccessDenied.handle(request, response, exception);
        write(response, HttpStatus.FORBIDDEN, "insufficient-scope", "Portée insuffisante",
                "Le jeton ne porte pas la portée requise pour cette opération.");
    }

    private void write(HttpServletResponse response, HttpStatus status, String type,
                       String title, String detail) throws IOException {
        String correlationId = UUID.randomUUID().toString();
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setType(URI.create(BASE_TYPE + type));
        problem.setTitle(title);
        problem.setProperty("correlationId", correlationId);

        log.warn("[{}] {} — {}", correlationId, status.value(), title);
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), problem);
    }
}
