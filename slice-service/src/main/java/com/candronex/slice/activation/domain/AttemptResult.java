package com.candronex.slice.activation.domain;

/** Résultat d'une tentative ; absent tant que le réseau n'a pas répondu. */
public enum AttemptResult {
    SUCCESS,
    FAILED,
    UNKNOWN
}
