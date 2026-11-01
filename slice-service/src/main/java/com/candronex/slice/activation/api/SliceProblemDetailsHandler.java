package com.candronex.slice.activation.api;

import com.candronex.platform.web.Problems;
import com.candronex.slice.activation.domain.ServiceNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Erreurs du domaine Activation en réponses RFC 9457. */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
class SliceProblemDetailsHandler {

    @ExceptionHandler(ServiceNotFoundException.class)
    ProblemDetail onNotFound(ServiceNotFoundException exception, HttpServletRequest request) {
        return Problems.of(request, HttpStatus.NOT_FOUND, "resource-not-found",
                "Ressource introuvable", exception.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail onIllegalArgument(IllegalArgumentException exception, HttpServletRequest request) {
        return Problems.of(request, HttpStatus.BAD_REQUEST, "bad-request",
                "Requête invalide", exception.getMessage());
    }
}
