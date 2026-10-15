package com.candronex.servicecatalog.domain;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Racine d'agrégat du contexte ServiceCatalog (TMF633) : un service que le client peut commander,
 * et les caractéristiques qui le décrivent.
 *
 * <p>La version change avec l'offre ; une commande fige celle qu'elle a reçue.
 */
public final class ServiceSpecification {

    private final ServiceSpecificationId id;
    private final String name;
    private final int version;
    private final List<CharacteristicSpecification> characteristics;

    private ServiceSpecification(ServiceSpecificationId id, String name, int version,
                                 List<CharacteristicSpecification> characteristics) {
        if (id == null) {
            throw new IllegalArgumentException("L'identifiant de la spécification est obligatoire.");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Le nom de la spécification est obligatoire.");
        }
        if (version < 1) {
            throw new IllegalArgumentException("La version d'une spécification commence à 1.");
        }
        if (characteristics == null || characteristics.isEmpty()) {
            throw new IllegalArgumentException("Une spécification décrit au moins une caractéristique.");
        }
        Set<String> names = new HashSet<>();
        for (CharacteristicSpecification characteristic : characteristics) {
            if (!names.add(characteristic.name())) {
                throw new IllegalArgumentException(
                        "Caractéristique en double : " + characteristic.name() + ".");
            }
        }
        this.id = id;
        this.name = name;
        this.version = version;
        this.characteristics = List.copyOf(characteristics);
    }

    /** Reconstitue une spécification publiée ; le catalogue est alimenté par migration. */
    public static ServiceSpecification restore(ServiceSpecificationId id, String name, int version,
                                               List<CharacteristicSpecification> characteristics) {
        return new ServiceSpecification(id, name, version, characteristics);
    }

    public ServiceSpecificationId id() {
        return id;
    }

    public String name() {
        return name;
    }

    public int version() {
        return version;
    }

    /** Dans l'ordre de publication ; liste non modifiable. */
    public List<CharacteristicSpecification> characteristics() {
        return characteristics;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ServiceSpecification that)) {
            return false;
        }
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
