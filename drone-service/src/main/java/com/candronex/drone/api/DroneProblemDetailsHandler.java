package com.candronex.drone.api;

import com.candronex.drone.domain.DroneNotFoundException;
import com.candronex.drone.domain.DuplicateIccidException;
import com.candronex.drone.domain.DuplicateImsiException;
import com.candronex.drone.domain.InvalidNetworkIdentityException;
import com.candronex.platform.web.Problems;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Erreurs du domaine Drone en réponses RFC 9457. */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
class DroneProblemDetailsHandler {

    @ExceptionHandler(DroneNotFoundException.class)
    ProblemDetail onNotFound(DroneNotFoundException exception, HttpServletRequest request) {
        return Problems.of(request, HttpStatus.NOT_FOUND, "resource-not-found",
                "Ressource introuvable", exception.getMessage());
    }

    @ExceptionHandler(DuplicateImsiException.class)
    ProblemDetail onDuplicateImsi(DuplicateImsiException exception, HttpServletRequest request) {
        return Problems.of(request, HttpStatus.CONFLICT, "duplicate-imsi",
                "IMSI déjà enregistré", exception.getMessage());
    }

    @ExceptionHandler(DuplicateIccidException.class)
    ProblemDetail onDuplicateIccid(DuplicateIccidException exception, HttpServletRequest request) {
        return Problems.of(request, HttpStatus.CONFLICT, "duplicate-iccid",
                "ICCID déjà enregistré", exception.getMessage());
    }

    // Deux enregistrements simultanés : la contrainte UNIQUE départage.
    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail onIntegrityViolation(DataIntegrityViolationException exception,
                                       HttpServletRequest request) {
        return Problems.of(request, HttpStatus.CONFLICT, "conflict",
                "Conflit", "Cet IMSI ou cet ICCID est déjà associé à un drone.");
    }

    @ExceptionHandler(InvalidNetworkIdentityException.class)
    ProblemDetail onInvalidNetworkIdentity(InvalidNetworkIdentityException exception,
                                           HttpServletRequest request) {
        return Problems.of(request, HttpStatus.UNPROCESSABLE_ENTITY, "invalid-network-identity",
                "Identité réseau invalide", exception.getMessage());
    }
}
