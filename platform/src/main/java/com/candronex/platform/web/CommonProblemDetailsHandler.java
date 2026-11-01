package com.candronex.platform.web;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/** Erreurs communes aux services ; celles du domaine sont traduites par chaque service. */
@RestControllerAdvice
@Order(Ordered.LOWEST_PRECEDENCE)
public class CommonProblemDetailsHandler {

    private static final Logger log = LoggerFactory.getLogger(CommonProblemDetailsHandler.class);
    private static final String BAD_REQUEST = "bad-request";
    private static final String BAD_REQUEST_TITLE = "Requête invalide";

    // ── 400 — la requête n'est pas comprise ──────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail onInvalidBody(MethodArgumentNotValidException exception, HttpServletRequest request) {
        String details = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + " : " + error.getDefaultMessage())
                .collect(Collectors.joining(" ; "));
        return Problems.of(request, HttpStatus.BAD_REQUEST, BAD_REQUEST, BAD_REQUEST_TITLE, details);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ProblemDetail onUnreadableBody(HttpMessageNotReadableException exception, HttpServletRequest request) {
        return Problems.of(request, HttpStatus.BAD_REQUEST, BAD_REQUEST, BAD_REQUEST_TITLE,
                "Le corps JSON est mal formé ou contient une valeur hors du contrat.");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ProblemDetail onMalformedParameter(MethodArgumentTypeMismatchException exception,
                                       HttpServletRequest request) {
        return Problems.of(request, HttpStatus.BAD_REQUEST, BAD_REQUEST, BAD_REQUEST_TITLE,
                "Le paramètre " + exception.getName() + " n'a pas le format attendu.");
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    ProblemDetail onMissingHeader(MissingRequestHeaderException exception, HttpServletRequest request) {
        return Problems.of(request, HttpStatus.BAD_REQUEST, BAD_REQUEST, BAD_REQUEST_TITLE,
                "L'en-tête " + exception.getHeaderName() + " est obligatoire.");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    ProblemDetail onMissingParameter(MissingServletRequestParameterException exception,
                                     HttpServletRequest request) {
        return Problems.of(request, HttpStatus.BAD_REQUEST, BAD_REQUEST, BAD_REQUEST_TITLE,
                "Le paramètre " + exception.getParameterName() + " est obligatoire.");
    }

    // ── 404 et 405 — route inconnue ──────────────────────────────────

    @ExceptionHandler(NoResourceFoundException.class)
    ProblemDetail onUnknownRoute(NoResourceFoundException exception, HttpServletRequest request) {
        return Problems.of(request, HttpStatus.NOT_FOUND, "resource-not-found",
                "Ressource introuvable", "Aucune ressource à cette adresse.");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    ProblemDetail onUnsupportedMethod(HttpRequestMethodNotSupportedException exception,
                                      HttpServletRequest request) {
        return Problems.of(request, HttpStatus.METHOD_NOT_ALLOWED, "method-not-allowed",
                "Méthode non permise", "La méthode " + exception.getMethod() + " n'est pas permise ici.");
    }

    // ── 415 — format de corps non accepté ────────────────────────────

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    ProblemDetail onUnsupportedMediaType(HttpMediaTypeNotSupportedException exception,
                                         HttpServletRequest request) {
        return Problems.of(request, HttpStatus.UNSUPPORTED_MEDIA_TYPE, "unsupported-media-type",
                "Type de contenu non pris en charge", "Le corps doit être en application/json.");
    }

    // ── 500 — tout le reste ──────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    ProblemDetail onUnexpected(Exception exception, HttpServletRequest request) {
        String correlationId = Problems.correlationId();
        log.error("status=500 error=internal-error", exception);
        return Problems.build(request.getRequestURI(), correlationId, HttpStatus.INTERNAL_SERVER_ERROR,
                "internal-error", "Erreur interne",
                "Une erreur inattendue est survenue. Citez le correlationId au support.");
    }
}
