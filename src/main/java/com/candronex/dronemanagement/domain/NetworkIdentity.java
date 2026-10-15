package com.candronex.dronemanagement.domain;

/**
 * Ce qui identifie un drone auprès du réseau mobile : son abonnement (IMSI), son support (ICCID)
 * et le type de ce support.
 */
public record NetworkIdentity(Imsi imsi, Iccid iccid, SimType simType) {

    public NetworkIdentity {
        if (imsi == null) {
            throw new InvalidNetworkIdentityException("L'IMSI est obligatoire.");
        }
        if (iccid == null) {
            throw new InvalidNetworkIdentityException("L'ICCID est obligatoire.");
        }
        if (simType == null) {
            throw new InvalidNetworkIdentityException("Le type de SIM est obligatoire.");
        }
    }

    /** Valide chaque composant ; une identité mal formée ne peut pas exister en mémoire. */
    public static NetworkIdentity of(String imsi, String iccid, SimType simType) {
        return new NetworkIdentity(Imsi.of(imsi), Iccid.of(iccid), simType);
    }
}
