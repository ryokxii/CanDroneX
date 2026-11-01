package com.candronex.order.domain;

import java.util.Collection;
import java.util.HashSet;

/**
 * Service de domaine : la même clé ne désigne la même demande que si le contenu est le même.
 * L'ordre des lignes n'entre pas dans la comparaison.
 */
public final class IdempotencyPolicy {

    /** @throws IdempotencyKeyConflictException si la clé a servi pour d'autres lignes */
    public void verifyReplay(Collection<OrderLineKey> placedLines, Collection<OrderLineKey> requestedLines) {
        boolean sameLines = requestedLines.size() == placedLines.size()
                && new HashSet<>(requestedLines).equals(new HashSet<>(placedLines));
        if (!sameLines) {
            throw new IdempotencyKeyConflictException();
        }
    }
}
