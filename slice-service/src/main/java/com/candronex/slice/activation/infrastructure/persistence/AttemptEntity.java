package com.candronex.slice.activation.infrastructure.persistence;

import com.candronex.slice.activation.domain.ActivationAttempt;
import com.candronex.slice.activation.domain.AttemptResult;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/** Une ligne de activation.activation_attempts. */
@Entity
@Table(schema = "activation", name = "activation_attempts")
class AttemptEntity {

    @Id
    @Column(name = "attempt_id", nullable = false, updatable = false)
    private UUID attemptId;

    @Column(name = "service_id", nullable = false, updatable = false)
    private UUID serviceId;

    @Column(name = "accepted_at", nullable = false, updatable = false)
    private Instant acceptedAt;

    @Column(name = "activated_at")
    private Instant activatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "result", length = 8)
    private AttemptResult result;

    /** Requis par l'ORM. */
    protected AttemptEntity() {
    }

    static AttemptEntity from(UUID serviceId, ActivationAttempt attempt) {
        AttemptEntity entity = new AttemptEntity();
        entity.attemptId = attempt.attemptId();
        entity.serviceId = serviceId;
        entity.acceptedAt = attempt.acceptedAt();
        entity.activatedAt = attempt.activatedAt();
        entity.result = attempt.result();
        return entity;
    }

    ActivationAttempt toDomain() {
        return ActivationAttempt.restore(attemptId, acceptedAt, activatedAt, result);
    }
}
