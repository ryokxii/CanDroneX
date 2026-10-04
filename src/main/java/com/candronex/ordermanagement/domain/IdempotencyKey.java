package com.candronex.ordermanagement.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Objects;

/** Clé fournie par le client pour identifier sa demande, et non sa requête. */
@Embeddable
public class IdempotencyKey {

    private static final int MAX_LENGTH = 64;

    @Column(name = "idempotency_key", nullable = false, length = MAX_LENGTH)
    private String value;

    /** Requis par l'ORM ; jamais appelé par le domaine. */
    protected IdempotencyKey() {
    }

    private IdempotencyKey(String value) {
        this.value = value;
    }

    public static IdempotencyKey of(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("La clé d'idempotence est obligatoire.");
        }
        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "La clé d'idempotence dépasse " + MAX_LENGTH + " caractères.");
        }
        return new IdempotencyKey(value);
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof IdempotencyKey that)) {
            return false;
        }
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
