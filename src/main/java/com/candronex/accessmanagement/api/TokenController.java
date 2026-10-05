package com.candronex.accessmanagement.api;

import com.candronex.accessmanagement.application.ClientCredentialsGrant;
import com.candronex.accessmanagement.application.InvalidClientException;
import com.candronex.accessmanagement.application.InvalidScopeException;
import com.candronex.accessmanagement.application.TokenIssuanceService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Point de terminaison de jeton OAuth 2.0, flux client_credentials uniquement : l'API est B2B, de
 * machine à machine, sans utilisateur final à rediriger.
 */
@RestController
class TokenController {

    static final String TOKEN_PATH = "/oauth2/token";
    private static final String CLIENT_CREDENTIALS = "client_credentials";
    private static final Logger log = LoggerFactory.getLogger(TokenController.class);

    private final TokenIssuanceService tokens;

    TokenController(TokenIssuanceService tokens) {
        this.tokens = tokens;
    }

    @PostMapping(path = TOKEN_PATH, consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    ResponseEntity<?> token(
            HttpServletRequest request,
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorization,
            @RequestParam(name = "grant_type", required = false) String grantType,
            @RequestParam(name = "scope", required = false) String scope,
            @RequestParam(name = "client_id", required = false) String clientId,
            @RequestParam(name = "client_secret", required = false) String clientSecret) {

        rejectSecretsInQueryString(request);
        if (grantType == null || grantType.isBlank()) {
            throw new InvalidTokenRequestException("invalid_request", "grant_type est obligatoire.");
        }
        if (!CLIENT_CREDENTIALS.equals(grantType)) {
            throw new InvalidTokenRequestException("unsupported_grant_type",
                    "Seul le flux client_credentials est pris en charge.");
        }

        ClientAuthentication client = ClientAuthentication.resolve(authorization, clientId, clientSecret);
        TokenResponse body;
        try {
            body = TokenResponse.from(tokens.issue(new ClientCredentialsGrant(
                    client.clientId(), client.clientSecret(), parseScopes(scope))));
        } catch (InvalidClientException rejected) {
            log.warn("Échec d'authentification OAuth 2.0 du client {}",
                    client.clientId().replaceAll("[\\r\\n]", "_"));
            return invalidClient(client.viaBasic());
        }

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(body);
    }

    // Erreurs au format OAuth 2.0 ; ces gestionnaires priment sur ProblemDetailsHandler.

    @ExceptionHandler(InvalidScopeException.class)
    ResponseEntity<TokenErrorResponse> onInvalidScope(InvalidScopeException exception) {
        return error(HttpStatus.BAD_REQUEST, "invalid_scope", exception.getMessage());
    }

    @ExceptionHandler(InvalidTokenRequestException.class)
    ResponseEntity<TokenErrorResponse> onInvalidRequest(InvalidTokenRequestException exception) {
        if ("invalid_client".equals(exception.error())) {
            return invalidClient(false);
        }
        return error(HttpStatus.BAD_REQUEST, exception.error(), exception.getMessage());
    }

    /** Défi Basic seulement si le client s'est présenté par l'en-tête Authorization. */
    private static ResponseEntity<TokenErrorResponse> invalidClient(boolean viaBasic) {
        ResponseEntity.BodyBuilder builder = ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .cacheControl(CacheControl.noStore());
        if (viaBasic) {
            builder.header(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"candronex\"");
        }
        return builder.body(new TokenErrorResponse("invalid_client",
                "Authentification du client échouée."));
    }

    private static ResponseEntity<TokenErrorResponse> error(HttpStatus status, String code,
                                                            String description) {
        return ResponseEntity.status(status)
                .cacheControl(CacheControl.noStore())
                .body(new TokenErrorResponse(code, description));
    }

    /** Le secret ne doit jamais transiter dans l'URL. */
    private static void rejectSecretsInQueryString(HttpServletRequest request) {
        String query = request.getQueryString();
        if (query != null && query.contains("client_secret")) {
            throw new InvalidTokenRequestException("invalid_request",
                    "Les identifiants du client ne doivent pas figurer dans l'URL.");
        }
    }

    private static Set<String> parseScopes(String scope) {
        if (scope == null || scope.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(scope.trim().split("\\s+")).collect(Collectors.toUnmodifiableSet());
    }
}
