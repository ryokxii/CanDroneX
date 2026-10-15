package com.candronex.servicecatalog.domain;

/** Type de la valeur d'une caractéristique ; toutes sont transportées sous forme de texte. */
public enum CharacteristicValueType {

    /** sst, fiveQi, arp. */
    INTEGER,

    /** sd, dnn, et les débits exprimés « valeur unité », comme 20 Mbps. */
    STRING;

    boolean accepts(String value) {
        if (this == STRING) {
            return true;
        }
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException notAnInteger) {
            return false;
        }
    }
}
