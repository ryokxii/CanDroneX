package com.candronex.droneregistration;

import com.candronex.droneregistration.domain.InvalidNetworkIdentityException;
import com.candronex.droneregistration.domain.NetworkIdentity;
import com.candronex.droneregistration.domain.SimType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * L'identité réseau porte ses propres règles : un IMSI mal formé ne peut pas exister en mémoire,
 * même le temps d'un instant.
 */
class NetworkIdentityTest {

    private static final String VALID_IMSI = "302720123456789";

    @Test
    @DisplayName("accepte un IMSI de 15 chiffres avec un type de SIM")
    void acceptsValidIdentity() {
        NetworkIdentity identity = NetworkIdentity.of(VALID_IMSI, SimType.ESIM);

        assertThat(identity.imsi()).isEqualTo(VALID_IMSI);
        assertThat(identity.simType()).isEqualTo(SimType.ESIM);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("refuse un IMSI absent")
    void rejectsMissingImsi(String imsi) {
        assertThatThrownBy(() -> NetworkIdentity.of(imsi, SimType.SIM))
                .isInstanceOf(InvalidNetworkIdentityException.class)
                .hasMessageContaining("obligatoire");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "30272012345678",     // 14 chiffres
            "3027201234567890",   // 16 chiffres
            "30272012345678X",    // caractère non numérique
            "302 720123456789"    // espace
    })
    @DisplayName("refuse un IMSI mal formé")
    void rejectsMalformedImsi(String imsi) {
        assertThatThrownBy(() -> NetworkIdentity.of(imsi, SimType.SIM))
                .isInstanceOf(InvalidNetworkIdentityException.class)
                .hasMessageContaining("15 chiffres");
    }

    @Test
    @DisplayName("refuse un type de SIM absent")
    void rejectsMissingSimType() {
        assertThatThrownBy(() -> NetworkIdentity.of(VALID_IMSI, null))
                .isInstanceOf(InvalidNetworkIdentityException.class);
    }

    @Test
    @DisplayName("deux identités de même contenu sont égales — objet-valeur")
    void comparesByValue() {
        assertThat(NetworkIdentity.of(VALID_IMSI, SimType.SIM))
                .isEqualTo(NetworkIdentity.of(VALID_IMSI, SimType.SIM))
                .isNotEqualTo(NetworkIdentity.of(VALID_IMSI, SimType.ESIM));
    }
}
