package com.candronex.accessmanagement.api;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Réponse d'erreur du point de terminaison de jeton. */
record TokenErrorResponse(
        @JsonProperty("error") String error,
        @JsonProperty("error_description") String errorDescription) {
}
