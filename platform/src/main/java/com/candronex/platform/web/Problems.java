package com.candronex.platform.web;

import com.candronex.platform.logging.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;

import java.net.URI;
import java.util.UUID;

/** Réponses RFC 9457, avec l'extension correlationId. */
public final class Problems {

    private static final Logger log = LoggerFactory.getLogger(Problems.class);
    private static final String BASE_TYPE = "https://candronex.ca/problems/";

    private Problems() {
    }

    public static ProblemDetail of(HttpServletRequest request, HttpStatusCode status,
                                   String type, String title, String detail) {
        String correlationId = correlationId();
        log.warn("status={} error={} detail=\"{}\"", status.value(), type, detail);
        return build(request.getRequestURI(), correlationId, status, type, title, detail);
    }

    public static ProblemDetail build(String instance, String correlationId, HttpStatusCode status,
                                      String type, String title, String detail) {
        ProblemDetail problem = ProblemDetail.forStatus(status);
        problem.setType(URI.create(BASE_TYPE + type));
        problem.setTitle(title);
        problem.setDetail(detail);
        problem.setInstance(URI.create(instance));
        problem.setProperty("correlationId", correlationId);
        return problem;
    }

    public static String correlationId() {
        String current = MDC.get(CorrelationIdFilter.MDC_KEY);
        return current != null ? current : UUID.randomUUID().toString();
    }
}
