package com.candronex.ordermanagement.domain;

/**
 * État global d'une commande, toujours dérivé de l'état de ses lignes par OrderStatusPolicy ;
 * jamais saisi.
 */
public enum OrderStatus {

    /** Toutes les lignes sont reçues ; aucun traitement n'a commencé. */
    RECEIVED,

    /** Au moins une ligne attend encore un résultat. */
    IN_PROGRESS,

    /** Toutes les lignes sont rendues. */
    COMPLETED,

    /** Certaines lignes rendues, les autres en échec ou annulées (TMF641 : partial). */
    PARTIAL,

    /** Aucune ligne rendue, et au moins une en échec. */
    FAILED,

    /** Toutes les lignes annulées avant d'avoir été traitées. */
    CANCELLED;

    /** Vrai tant qu'une ligne attend un résultat ; la commande n'a alors pas de date d'achèvement. */
    public boolean isPending() {
        return this == RECEIVED || this == IN_PROGRESS;
    }
}
