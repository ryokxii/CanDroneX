package com.candronex.droneregistration.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.util.Objects;
import java.util.regex.Pattern;

/** Ce qui identifie un drone auprès du réseau mobile : son IMSI et son type de SIM. */
@Embeddable
public class NetworkIdentity {

    private static final Pattern IMSI_FORMAT = Pattern.compile("^[0-9]{15}$");

    @Column(name = "imsi", nullable = false, length = 15)
    private String imsi;

    @Enumerated(EnumType.STRING)
    @Column(name = "sim_type", nullable = false, length = 8)
    private SimType simType;

    /** Requis par l'ORM ; jamais appelé par le domaine. */
    protected NetworkIdentity() {
    }

    private NetworkIdentity(String imsi, SimType simType) {
        this.imsi = imsi;
        this.simType = simType;
    }

    /** Seule façon de construire une identité réseau. */
    public static NetworkIdentity of(String imsi, SimType simType) {
        if (imsi == null || imsi.isBlank()) {
            throw new InvalidNetworkIdentityException("L'IMSI est obligatoire.");
        }
        if (!IMSI_FORMAT.matcher(imsi).matches()) {
            throw new InvalidNetworkIdentityException(
                    "L'IMSI doit comporter exactement 15 chiffres.");
        }
        if (simType == null) {
            throw new InvalidNetworkIdentityException("Le type de SIM est obligatoire.");
        }
        return new NetworkIdentity(imsi, simType);
    }

    public String imsi() {
        return imsi;
    }

    public SimType simType() {
        return simType;
    }

    /** Égalité par valeur : deux identités de même contenu sont la même. */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof NetworkIdentity that)) {
            return false;
        }
        return Objects.equals(imsi, that.imsi) && simType == that.simType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(imsi, simType);
    }

    @Override
    public String toString() {
        return "NetworkIdentity[imsi=%s, simType=%s]".formatted(imsi, simType);
    }
}
