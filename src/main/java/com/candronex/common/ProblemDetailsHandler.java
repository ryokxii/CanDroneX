package com.candronex.common;

import com.candronex.accessmanagement.published.UnknownClientException;
import com.candronex.droneregistration.domain.DuplicateImsiException;
import com.candronex.droneregistration.domain.InvalidNetworkIdentityException;
import com.candronex.ordermanagement.domain.DroneNotFoundException;
import com.candronex.ordermanagement.domain.DuplicateOrderLineException;
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

import java.net.URI;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.stream.Collectors;

/** Traduit les exceptions en réponses d'erreur, en un seul endroit. */
@RestControllerAdvice
class ProblemDetailsHandler {

    private static final Logger log = LoggerFactory.getLogger(ProblemDetailsHandler.class);
    private static final String BASE_TYPE = "https://candronex.ca/problems/";

    // ── 400 — la requête n'est pas comprise ──────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail onInvalidBody(MethodArgumentNotValidException exception) {
        String details = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + " : " + error.getDefaultMessage())
                .collect(Collectors.joining(" ; "));
        return problem(HttpStatus.BAD_REQUEST, "validation",
                "Requête invalide", details);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail onUnreadableBody(HttpMessageNotReadableException exception) {
        // Couvre notamment un serviceType hors de l'offre publiée.
        return problem(HttpStatus.BAD_REQUEST, "malformed-request",
                "Corps de requête illisible",
                "Le corps JSON est mal formé ou contient une valeur hors du contrat.");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail onIllegalArgument(IllegalArgumentException exception) {
        return problem(HttpStatus.BAD_REQUEST, "invalid-argument",
                "Requête invalide", exception.getMessage());
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    ProblemDetail onMissingHeader(MissingRequestHeaderException exception) {
        return problem(HttpStatus.BAD_REQUEST, "missing-header",
                "En-tête obligatoire absent",
                "L'en-tête " + exception.getHeaderName() + " est obligatoire.");
    }

    // ── 401 — l'appelant n'est pas identifié ─────────────────────────
    // Jeton valide mais client disparu ; les autres 401 viennent de la chaîne de sécurité.

    @ExceptionHandler(UnknownClientException.class)
    ProblemDetail onUnknownClient(UnknownClientException exception) {
        return problem(HttpStatus.UNAUTHORIZED, "unknown-client",
                "Client inconnu", "Ce client n'est pas enregistré sur la plateforme.");
    }

    // ── 404 — la ressource n'existe pas, ou n'est pas la vôtre ───────
    // Les deux cas sont volontairement indiscernables.

    @ExceptionHandler({DroneNotFoundException.class, NoSuchElementException.class})
    ProblemDetail onNotFound(RuntimeException exception) {
        return problem(HttpStatus.NOT_FOUND, "resource-not-found",
                "Ressource introuvable", exception.getMessage());
    }

    // ── 409 — conflit d'unicité ──────────────────────────────────────

    @ExceptionHandler(DuplicateImsiException.class)
    ProblemDetail onDuplicateImsi(DuplicateImsiException exception) {
        return problem(HttpStatus.CONFLICT, "duplicate-imsi",
                "IMSI déjà enregistré", exception.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail onIntegrityViolation(DataIntegrityViolationException exception) {
        // Le message SQL reste dans les journaux : il nomme des tables.
        return problem(HttpStatus.CONFLICT, "conflict",
                "Conflit", "La ressource existe déjà ou viole une contrainte d'unicité.");
    }

    // ── 422 — comprise, mais refusée par une règle du domaine ────────

    @ExceptionHandler(InvalidNetworkIdentityException.class)
    ProblemDetail onInvalidNetworkIdentity(InvalidNetworkIdentityException exception) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, "invalid-network-identity",
                "Identité réseau invalide", exception.getMessage());
    }

    @ExceptionHandler(DuplicateOrderLineException.class)
    ProblemDetail onDuplicateOrderLine(DuplicateOrderLineException exception) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, "duplicate-order-line",
                "Ligne de commande en double", exception.getMessage());
    }

    // ── 415 — format de corps non accepté ────────────────────────────

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ProblemDetail onUnsupportedMediaType(HttpMediaTypeNotSupportedException exception) {
        return problem(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "unsupported-media-type",
                "Type de contenu non pris en charge",
                "Types acceptés : " + exception.getSupportedMediaTypes());
    }

    // ── 500 — tout le reste ──────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    ProblemDetail onUnexpected(Exception exception) {
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "internal-error",
                "Erreur interne",
                "Une erreur inattendue est survenue. Citez le correlationId au support.");
    }

    /** Construit la réponse et journalise la cause avec le même identifiant. */
    private ProblemDetail problem(HttpStatusCode status, String type,
                                  String title, String detail) {
        String correlationId = UUID.randomUUID().toString();

        ProblemDetail problem = ProblemDetail.forStatus(status);
        problem.setType(URI.create(BASE_TYPE + type));
        problem.setTitle(title);
        problem.setDetail(detail);
        problem.setProperty("correlationId", correlationId);

        log.warn("[{}] {} — {} : {}", correlationId, status.value(), title, detail);
        return problem;
    }
}
