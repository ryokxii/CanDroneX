package com.candronex.dronemanagement.domain;

import java.util.regex.Pattern;

/** Ce qui identifie un drone auprès du réseau mobile : son IMSI et son type de SIM. */
public record NetworkIdentity(String imsi, SimType simType) {

    private static final Pattern IMSI_FORMAT = Pattern.compile("^[0-9]{15}$");

    public NetworkIdentity {
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
    }

    /** Seule façon de construire une identité réseau. */
    public static NetworkIdentity of(String imsi, SimType simType) {
        return new NetworkIdentity(imsi, simType);
    }
}
