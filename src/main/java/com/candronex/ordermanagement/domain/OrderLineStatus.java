package com.candronex.ordermanagement.domain;

/**
 * État d'une ligne de commande : le traitement d'un service pour un drone.
 *
 * <p>Distinct d'OrderStatus : une ligne ne peut pas être partielle. Un délai dépassé lors de
 * l'activation n'est pas un échec : la ligne reste IN_PROGRESS tant que ServiceActivation n'a pas
 * vérifié l'état réel du réseau.
 */
public enum OrderLineStatus {

    /** Acceptée avec la commande ; l'activation n'est pas encore demandée. */
    RECEIVED,

    /** L'activation est demandée ; son résultat n'est pas encore connu. */
    IN_PROGRESS,

    /** Le service est actif sur le réseau. */
    COMPLETED,

    /** Le réseau a refusé l'activation ; une reprise (UC-10) peut la relancer. */
    FAILED,

    /** Annulée avant toute activation (UC-15). */
    CANCELLED;

    /** Vrai tant que la ligne attend un résultat. */
    public boolean isPending() {
        return this == RECEIVED || this == IN_PROGRESS;
    }

    /** Transitions permises ; toute autre est une erreur de programmation. */
    public boolean canTransitionTo(OrderLineStatus next) {
        return switch (this) {
            case RECEIVED -> next == IN_PROGRESS || next == CANCELLED;
            case IN_PROGRESS -> next == COMPLETED || next == FAILED;
            case FAILED -> next == IN_PROGRESS;
            case COMPLETED, CANCELLED -> false;
        };
    }
}
