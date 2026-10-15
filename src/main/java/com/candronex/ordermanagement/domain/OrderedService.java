package com.candronex.ordermanagement.domain;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Le service commandé par une ligne (TMF641 service) : la spécification du catalogue, sa version et
 * les valeurs de ses caractéristiques, copiées à la soumission.
 *
 * <p>Une valeur, jamais une référence : si l'offre évolue, ce qui a été commandé ne change pas.
 */
public record OrderedService(
        String serviceSpecificationId,
        int serviceSpecificationVersion,
        List<ServiceCharacteristic> characteristics) {

    public OrderedService {
        if (serviceSpecificationId == null || serviceSpecificationId.isBlank()) {
            throw new IllegalArgumentException("La spécification du service est obligatoire.");
        }
        if (serviceSpecificationVersion < 1) {
            throw new IllegalArgumentException("La version d'une spécification commence à 1.");
        }
        if (characteristics == null || characteristics.isEmpty()) {
            throw new IllegalArgumentException("Le service commandé porte au moins une caractéristique.");
        }
        Set<String> names = new HashSet<>();
        for (ServiceCharacteristic characteristic : characteristics) {
            if (!names.add(characteristic.name())) {
                throw new IllegalArgumentException(
                        "Caractéristique en double : " + characteristic.name() + ".");
            }
        }
        characteristics = List.copyOf(characteristics);
    }
}
