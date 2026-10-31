package com.candronex.slice.activation.domain;

/** Une caractéristique reçue dans la demande d'activation (sst, sd, dnn, fiveQi, arp, débits). */
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
