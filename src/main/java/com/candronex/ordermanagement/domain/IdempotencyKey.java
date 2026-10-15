package com.candronex.ordermanagement.domain;

/** Clé fournie par le client pour identifier sa demande, et non sa requête. Portée : un client. */
public record IdempotencyKey(String value) {

    private static final int MAX_LENGTH = 64;

    public IdempotencyKey {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("La clé d'idempotence est obligatoire.");
        }
        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "La clé d'idempotence dépasse " + MAX_LENGTH + " caractères.");
        }
    }

    public static IdempotencyKey of(String value) {
        return new IdempotencyKey(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
