package com.candronex.ordermanagement.domain;

/** État d'une commande et de ses lignes. */
public enum OrderStatus {

    /** Enregistrée, pas encore prise en charge. */
    RECEIVED,

    /** Au moins une ligne est en cours d'activation. */
    IN_PROGRESS,

    /** Service rendu. */
    COMPLETED,

    /** Commande seulement : certaines lignes rendues, d'autres non. */
    PARTIALLY_COMPLETED,

    /** Activation définitivement en échec. */
    FAILED,

    /** Annulée avant d'avoir été honorée ; rien n'a été consommé. */
    CANCELLED;

    /** Un état terminal ne change plus : il n'attend aucune activation. */
    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED || this == CANCELLED;
    }
}
