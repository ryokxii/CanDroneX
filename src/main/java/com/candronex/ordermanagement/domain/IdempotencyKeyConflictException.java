package com.candronex.ordermanagement.domain;

/** La clé d'idempotence a déjà servi, pour ce client, à une commande au contenu différent. */
public class IdempotencyKeyConflictException extends RuntimeException {

    public IdempotencyKeyConflictException() {
        super("Cette clé d'idempotence a déjà servi pour une commande au contenu différent.");
    }
}
