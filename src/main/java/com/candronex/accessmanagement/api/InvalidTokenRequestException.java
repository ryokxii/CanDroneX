package com.candronex.accessmanagement.api;

/** Requête de jeton mal formée, avec son code d'erreur OAuth 2.0. */
class InvalidTokenRequestException extends RuntimeException {

    private final String error;

    InvalidTokenRequestException(String error, String description) {
        super(description);
        this.error = error;
    }

    String error() {
        return error;
    }
}
