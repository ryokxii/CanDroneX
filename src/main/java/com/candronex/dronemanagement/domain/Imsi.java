package com.candronex.dronemanagement.domain;

import java.util.regex.Pattern;

/**
 * Identité d'abonné du drone dans le réseau mobile : MCC + MNC + MSIN, 15 chiffres.
 *
 * <p>Identifiant sensible (Loi 25, LPRPDE) : il ne quitte pas le module DroneManagement et
 * n'apparaît jamais en clair dans une représentation textuelle.
 */
public record Imsi(String value) {

    private static final Pattern FORMAT = Pattern.compile("^[0-9]{15}$");
    private static final int VISIBLE_PREFIX = 5;

    public Imsi {
        if (value == null || value.isBlank()) {
            throw new InvalidNetworkIdentityException("L'IMSI est obligatoire.");
        }
        if (!FORMAT.matcher(value).matches()) {
            throw new InvalidNetworkIdentityException(
                    "L'IMSI doit comporter exactement 15 chiffres.");
        }
    }

    public static Imsi of(String value) {
        return new Imsi(value);
    }

    /** Seuls le MCC et le MNC restent lisibles, pour le diagnostic. */
    @Override
    public String toString() {
        return value.substring(0, VISIBLE_PREFIX) + "*".repeat(value.length() - VISIBLE_PREFIX);
    }
}
