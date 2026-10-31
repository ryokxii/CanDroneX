package com.candronex.order.api;

import com.candronex.order.application.DependencyUnavailableException;
import com.candronex.order.domain.DroneNotFoundException;
import com.candronex.order.domain.DuplicateOrderLineException;
import com.candronex.order.domain.IdempotencyKeyConflictException;
import com.candronex.order.domain.UnknownServiceSpecificationException;
import com.candronex.platform.web.Problems;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Erreurs du domaine Order en réponses RFC 9457. */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
class OrderProblemDetailsHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail onIllegalArgument(IllegalArgumentException exception, HttpServletRequest request) {
        return Problems.of(request, HttpStatus.BAD_REQUEST, "bad-request",
                "Requête invalide", exception.getMessage());
    }

    @ExceptionHandler(DroneNotFoundException.class)
    ProblemDetail onDroneNotFound(DroneNotFoundException exception, HttpServletRequest request) {
        return Problems.of(request, HttpStatus.NOT_FOUND, "resource-not-found",
                "Ressource introuvable", exception.getMessage());
    }

    @ExceptionHandler(IdempotencyKeyConflictException.class)
    ProblemDetail onIdempotencyKeyConflict(IdempotencyKeyConflictException exception,
                                           HttpServletRequest request) {
        return Problems.of(request, HttpStatus.CONFLICT, "idempotency-key-reused",
                "Clé d'idempotence déjà utilisée", exception.getMessage());
    }

    @ExceptionHandler(DuplicateOrderLineException.class)
    ProblemDetail onDuplicateOrderLine(DuplicateOrderLineException exception,
                                       HttpServletRequest request) {
        return Problems.of(request, HttpStatus.UNPROCESSABLE_ENTITY, "duplicate-order-line",
                "Ligne de commande en double", exception.getMessage());
    }

    @ExceptionHandler(UnknownServiceSpecificationException.class)
    ProblemDetail onUnknownServiceSpecification(UnknownServiceSpecificationException exception,
                                                HttpServletRequest request) {
        return Problems.of(request, HttpStatus.UNPROCESSABLE_ENTITY, "unknown-service-specification",
                "Service hors catalogue", exception.getMessage());
    }

    @ExceptionHandler(DependencyUnavailableException.class)
    ProblemDetail onDependencyUnavailable(DependencyUnavailableException exception,
                                          HttpServletRequest request) {
        return Problems.of(request, HttpStatus.SERVICE_UNAVAILABLE, "dependency-unavailable",
                "Service indisponible",
                "Le service des drones ne répond pas. Réessayez avec la même clé d'idempotence.");
    }
}
