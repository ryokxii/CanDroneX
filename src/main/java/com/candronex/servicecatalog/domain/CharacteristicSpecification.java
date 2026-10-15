package com.candronex.servicecatalog.domain;

/**
 * Ce qu'une caractéristique peut valoir dans l'offre — ici, sa valeur par défaut, que la commande
 * copie. Les valeurs effectivement commandées appartiennent à la commande, pas au catalogue.
 */
public record CharacteristicSpecification(String name, CharacteristicValueType valueType,
                                          String defaultValue) {

    public CharacteristicSpecification {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Le nom de la caractéristique est obligatoire.");
        }
        if (valueType == null) {
            throw new IllegalArgumentException("Le type de la caractéristique est obligatoire.");
        }
        if (defaultValue == null || defaultValue.isBlank()) {
            throw new IllegalArgumentException("La caractéristique " + name + " n'a pas de valeur.");
        }
        if (!valueType.accepts(defaultValue)) {
            throw new IllegalArgumentException(
                    "La valeur de " + name + " n'est pas du type " + valueType + ".");
        }
    }
}
