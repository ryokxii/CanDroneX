package com.candronex.slice.activation.domain;

/** État d'un service activé sur le réseau. */
public enum ServiceState {

    /** Enregistré ; l'appel au réseau n'a pas encore rendu de résultat. */
    PENDING,

    /** Le réseau a confirmé l'activation. */
    ACTIVE,

    /** Le réseau a refusé l'activation. */
    FAILED,

    /** Délai dépassé : issue inconnue, pas un échec. L'état réel est lu avant toute reprise. */
    UNCONFIRMED
}
