package com.candronex.common;

import com.candronex.customermanagement.published.UnknownClientException;
import com.candronex.dronemanagement.domain.DuplicateIccidException;
import com.candronex.dronemanagement.domain.DuplicateImsiException;
import com.candronex.dronemanagement.domain.InvalidNetworkIdentityException;
import com.candronex.ordermanagement.domain.DroneNotFoundException;
import com.candronex.ordermanagement.domain.DuplicateOrderLineException;
import com.candronex.ordermanagement.domain.IdempotencyKeyConflictException;
import com.candronex.ordermanagement.domain.UnknownServiceSpecificationException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.net.URI;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Traduit les exceptions en réponses RFC 9457, en un seul endroit : membres normalisés type, title,
 * status, detail et instance, plus l'extension correlationId. Aucun détail technique ni aucun
 * identifiant d'abonné n'y figure ; la cause est journalisée sous le même correlationId.
 */
@RestControllerAdvice
class ProblemDetailsHandler {

    private static final Logger log = LoggerFactory.getLogger(ProblemDetailsHandler.class);
    private static final String BASE_TYPE = "https://candronex.ca/problems/";

    // ── 400 — la requête n'est pas comprise ──────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail onInvalidBody(MethodArgumentNotValidException exception, HttpServletRequest request) {
        String details = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + " : " + error.getDefaultMessage())
                .collect(Collectors.joining(" ; "));
        return problem(request, HttpStatus.BAD_REQUEST, "validation", "Requête invalide", details);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail onUnreadableBody(HttpMessageNotReadableException exception, HttpServletRequest request) {
        return problem(request, HttpStatus.BAD_REQUEST, "malformed-request",
                "Corps de requête illisible",
                "Le corps JSON est mal formé ou contient une valeur hors du contrat.");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail onIllegalArgument(IllegalArgumentException exception, HttpServletRequest request) {
        return problem(request, HttpStatus.BAD_REQUEST, "invalid-argument",
                "Requête invalide", exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ProblemDetail onMalformedParameter(MethodArgumentTypeMismatchException exception,
                                       HttpServletRequest request) {
        return problem(request, HttpStatus.BAD_REQUEST, "malformed-parameter",
                "Paramètre mal formé",
                "Le paramètre " + exception.getName() + " n'a pas le format attendu.");
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    ProblemDetail onMissingHeader(MissingRequestHeaderException exception, HttpServletRequest request) {
        return problem(request, HttpStatus.BAD_REQUEST, "missing-header",
                "En-tête obligatoire absent",
                "L'en-tête " + exception.getHeaderName() + " est obligatoire.");
    }

    // ── 401 — l'appelant n'est pas identifié ─────────────────────────
    // Jeton valide mais client disparu ; les autres 401 viennent de la chaîne de sécurité.

    @ExceptionHandler(UnknownClientException.class)
    ProblemDetail onUnknownClient(UnknownClientException exception, HttpServletRequest request) {
        return problem(request, HttpStatus.UNAUTHORIZED, "unknown-client",
                "Client inconnu", "Ce client n'est pas enregistré sur la plateforme.");
    }

    // ── 404 — la ressource n'existe pas, ou n'est pas la vôtre ───────
    // Les deux cas sont volontairement indiscernables.

    @ExceptionHandler({DroneNotFoundException.class, NoSuchElementException.class})
    ProblemDetail onNotFound(RuntimeException exception, HttpServletRequest request) {
        return problem(request, HttpStatus.NOT_FOUND, "resource-not-found",
                "Ressource introuvable", exception.getMessage());
    }

    // ── 409 — conflit avec l'état existant ───────────────────────────

    @ExceptionHandler(DuplicateImsiException.class)
    ProblemDetail onDuplicateImsi(DuplicateImsiException exception, HttpServletRequest request) {
        return problem(request, HttpStatus.CONFLICT, "duplicate-imsi",
                "IMSI déjà enregistré", exception.getMessage());
    }

    @ExceptionHandler(DuplicateIccidException.class)
    ProblemDetail onDuplicateIccid(DuplicateIccidException exception, HttpServletRequest request) {
        return problem(request, HttpStatus.CONFLICT, "duplicate-iccid",
                "ICCID déjà enregistré", exception.getMessage());
    }

    @ExceptionHandler(IdempotencyKeyConflictException.class)
    ProblemDetail onIdempotencyKeyConflict(IdempotencyKeyConflictException exception,
                                           HttpServletRequest request) {
        return problem(request, HttpStatus.CONFLICT, "idempotency-key-reused",
                "Clé d'idempotence déjà utilisée", exception.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail onIntegrityViolation(DataIntegrityViolationException exception,
                                       HttpServletRequest request) {
        // Le message SQL n'est pas renvoyé : il nomme des tables et peut contenir des valeurs.
        return problem(request, HttpStatus.CONFLICT, "conflict",
                "Conflit", "La ressource existe déjà ou viole une contrainte d'unicité.");
    }

    // ── 422 — comprise, mais refusée par une règle du domaine ────────

    @ExceptionHandler(InvalidNetworkIdentityException.class)
    ProblemDetail onInvalidNetworkIdentity(InvalidNetworkIdentityException exception,
                                           HttpServletRequest request) {
        return problem(request, HttpStatus.UNPROCESSABLE_ENTITY, "invalid-network-identity",
                "Identité réseau invalide", exception.getMessage());
    }

    @ExceptionHandler(DuplicateOrderLineException.class)
    ProblemDetail onDuplicateOrderLine(DuplicateOrderLineException exception,
                                       HttpServletRequest request) {
        return problem(request, HttpStatus.UNPROCESSABLE_ENTITY, "duplicate-order-line",
                "Ligne de commande en double", exception.getMessage());
    }

    @ExceptionHandler(UnknownServiceSpecificationException.class)
    ProblemDetail onUnknownServiceSpecification(UnknownServiceSpecificationException exception,
                                                HttpServletRequest request) {
        return problem(request, HttpStatus.UNPROCESSABLE_ENTITY, "unknown-service-specification",
                "Service hors catalogue", exception.getMessage());
    }

    // ── 415 — format de corps non accepté ────────────────────────────

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ProblemDetail onUnsupportedMediaType(HttpMediaTypeNotSupportedException exception,
                                         HttpServletRequest request) {
        return problem(request, HttpStatus.UNSUPPORTED_MEDIA_TYPE, "unsupported-media-type",
                "Type de contenu non pris en charge",
                "Types acceptés : " + exception.getSupportedMediaTypes());
    }

    // ── 500 — tout le reste ──────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    ProblemDetail onUnexpected(Exception exception, HttpServletRequest request) {
        String correlationId = UUID.randomUUID().toString();
        log.error("[{}] Erreur inattendue", correlationId, exception);
        return build(request, correlationId, HttpStatus.INTERNAL_SERVER_ERROR, "internal-error",
                "Erreur interne",
                "Une erreur inattendue est survenue. Citez le correlationId au support.");
    }

    /** Construit la réponse et journalise la cause avec le même identifiant. */
    private static ProblemDetail problem(HttpServletRequest request, HttpStatusCode status,
                                         String type, String title, String detail) {
        String correlationId = UUID.randomUUID().toString();
        log.warn("[{}] {} — {} : {}", correlationId, status.value(), title, detail);
        return build(request, correlationId, status, type, title, detail);
    }

    private static ProblemDetail build(HttpServletRequest request, String correlationId,
                                       HttpStatusCode status, String type, String title,
                                       String detail) {
        ProblemDetail problem = ProblemDetail.forStatus(status);
        problem.setType(URI.create(BASE_TYPE + type));
        problem.setTitle(title);
        problem.setDetail(detail);
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("correlationId", correlationId);
        return problem;
    }
}
