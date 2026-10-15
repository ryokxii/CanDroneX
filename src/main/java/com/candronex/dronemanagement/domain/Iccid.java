package com.candronex.dronemanagement.domain;

import java.util.regex.Pattern;

/**
 * Numéro de série du support d'abonnement — carte SIM ou profil eSIM — selon UIT-T E.118 :
 * préfixe 89, 19 ou 20 chiffres, le dernier étant un chiffre de contrôle de Luhn.
 *
 * <p>Identifie le support, non l'abonnement ; traité avec la même prudence que l'IMSI.
 */
public record Iccid(String value) {

    private static final Pattern FORMAT = Pattern.compile("^89[0-9]{17,18}$");
    private static final int VISIBLE_SUFFIX = 4;

    public Iccid {
        if (value == null || value.isBlank()) {
            throw new InvalidNetworkIdentityException("L'ICCID est obligatoire.");
        }
        if (!FORMAT.matcher(value).matches()) {
            throw new InvalidNetworkIdentityException(
                    "L'ICCID doit comporter 19 ou 20 chiffres et commencer par 89.");
        }
        if (!hasValidCheckDigit(value)) {
            throw new InvalidNetworkIdentityException(
                    "Le chiffre de contrôle de l'ICCID est invalide.");
        }
    }

    public static Iccid of(String value) {
        return new Iccid(value);
    }

    /** Algorithme de Luhn : un chiffre sur deux est doublé, en partant du chiffre de contrôle. */
    private static boolean hasValidCheckDigit(String digits) {
        int sum = 0;
        boolean doubled = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int digit = digits.charAt(i) - '0';
            if (doubled) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }
            sum += digit;
            doubled = !doubled;
        }
        return sum % 10 == 0;
    }

    /** Seuls les quatre derniers chiffres restent lisibles, comme sur un relevé. */
    @Override
    public String toString() {
        int hidden = value.length() - VISIBLE_SUFFIX;
        return "*".repeat(hidden) + value.substring(hidden);
    }
}
