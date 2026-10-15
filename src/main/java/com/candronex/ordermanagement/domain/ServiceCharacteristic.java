package com.candronex.ordermanagement.domain;

/** Une caractéristique commandée et sa valeur, figées à la soumission (TMF641 serviceCharacteristic). */
public record ServiceCharacteristic(String name, String value) {

    public ServiceCharacteristic {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Le nom de la caractéristique est obligatoire.");
        }
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("La caractéristique " + name + " n'a pas de valeur.");
        }
    }
}
