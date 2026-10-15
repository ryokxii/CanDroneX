package com.candronex.servicecatalog.domain;

import java.util.regex.Pattern;

/** Identifiant stable d'une spécification de service : c'est lui que la commande référence. */
public record ServiceSpecificationId(String value) {

    private static final Pattern FORMAT = Pattern.compile("^[A-Z][A-Z0-9_]{0,31}$");

    public ServiceSpecificationId {
        if (value == null || !FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "Identifiant de spécification invalide : majuscules, chiffres et _, 32 au plus.");
        }
    }

    public static ServiceSpecificationId of(String value) {
        return new ServiceSpecificationId(value);
    }

    /** Vrai si la valeur peut désigner une spécification, sans lever d'exception. */
    public static boolean isWellFormed(String value) {
        return value != null && FORMAT.matcher(value).matches();
    }

    @Override
    public String toString() {
        return value;
    }
}
