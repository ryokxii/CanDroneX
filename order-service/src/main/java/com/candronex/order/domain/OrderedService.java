package com.candronex.order.domain;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Le service commandé par une ligne (TMF641 service) : la spécification du catalogue et les valeurs
 * de ses caractéristiques, copiées à la soumission.
 *
 * <p>Une valeur, jamais une référence : si l'offre évolue, ce qui a été commandé ne change pas.
 */
public record OrderedService(
        String serviceSpecificationId,
        List<ServiceCharacteristic> characteristics) {

    public OrderedService {
        if (serviceSpecificationId == null || serviceSpecificationId.isBlank()) {
            throw new IllegalArgumentException("La spécification du service est obligatoire.");
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
