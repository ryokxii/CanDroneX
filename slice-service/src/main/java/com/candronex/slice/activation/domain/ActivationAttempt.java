package com.candronex.slice.activation.domain;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Entité de l'agrégat Service : un appel au réseau ; acceptedAt et activatedAt servent au P95. */
public final class ActivationAttempt {

    private final UUID attemptId;
    private final Instant acceptedAt;
    private Instant activatedAt;
    private AttemptResult result;

    private ActivationAttempt(UUID attemptId, Instant acceptedAt, Instant activatedAt,
                              AttemptResult result) {
        this.attemptId = Objects.requireNonNull(attemptId, "attemptId");
        this.acceptedAt = Objects.requireNonNull(acceptedAt, "acceptedAt");
        this.activatedAt = activatedAt;
        this.result = result;
    }

    static ActivationAttempt accept(Instant acceptedAt) {
        return new ActivationAttempt(UUID.randomUUID(), acceptedAt, null, null);
    }

    /** Reconstitue une tentative enregistrée ; réservé à l'adaptateur de persistance. */
    public static ActivationAttempt restore(UUID attemptId, Instant acceptedAt, Instant activatedAt,
                                            AttemptResult result) {
        return new ActivationAttempt(attemptId, acceptedAt, activatedAt, result);
    }

    void finish(AttemptResult outcome, Instant now) {
        this.result = outcome;
        this.activatedAt = outcome == AttemptResult.SUCCESS ? now : null;
    }

    boolean isOpen() {
        return result == null || result == AttemptResult.UNKNOWN;
    }

    public UUID attemptId() {
        return attemptId;
    }

    public Instant acceptedAt() {
        return acceptedAt;
    }

    public Instant activatedAt() {
        return activatedAt;
    }

    public AttemptResult result() {
        return result;
    }
}
