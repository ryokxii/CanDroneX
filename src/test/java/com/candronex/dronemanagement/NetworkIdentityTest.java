package com.candronex.dronemanagement;

import com.candronex.dronemanagement.domain.Iccid;
import com.candronex.dronemanagement.domain.Imsi;
import com.candronex.dronemanagement.domain.InvalidNetworkIdentityException;
import com.candronex.dronemanagement.domain.NetworkIdentity;
import com.candronex.dronemanagement.domain.SimType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * L'identité réseau porte ses propres règles : un IMSI ou un ICCID mal formé ne peut pas exister en
 * mémoire, et aucun des deux n'apparaît en clair dans une représentation textuelle.
 */
class NetworkIdentityTest {

    /** PLMN d'essai 999-70 : aucun opérateur réel. */
    static final String VALID_IMSI = "999700000010231";

    /** Préfixe 89, 19 chiffres, chiffre de contrôle de Luhn valide. */
    static final String VALID_ICCID = "8999700000000010239";

    @Test
    @DisplayName("accepte un IMSI, un ICCID et un type de SIM valides")
    void acceptsValidIdentity() {
        NetworkIdentity identity = NetworkIdentity.of(VALID_IMSI, VALID_ICCID, SimType.ESIM);

        assertThat(identity.imsi().value()).isEqualTo(VALID_IMSI);
        assertThat(identity.iccid().value()).isEqualTo(VALID_ICCID);
        assertThat(identity.simType()).isEqualTo(SimType.ESIM);
    }

    @Test
    @DisplayName("refuse un type de SIM absent")
    void rejectsMissingSimType() {
        assertThatThrownBy(() -> NetworkIdentity.of(VALID_IMSI, VALID_ICCID, null))
                .isInstanceOf(InvalidNetworkIdentityException.class);
    }

    @Test
    @DisplayName("deux identités de même contenu sont égales — objet-valeur")
    void comparesByValue() {
        assertThat(NetworkIdentity.of(VALID_IMSI, VALID_ICCID, SimType.SIM))
                .isEqualTo(NetworkIdentity.of(VALID_IMSI, VALID_ICCID, SimType.SIM))
                .isNotEqualTo(NetworkIdentity.of(VALID_IMSI, VALID_ICCID, SimType.ESIM));
    }

    @Test
    @DisplayName("ni l'IMSI ni l'ICCID n'apparaissent en clair dans la représentation textuelle")
    void masksIdentifiersInTextualRepresentation() {
        String text = NetworkIdentity.of(VALID_IMSI, VALID_ICCID, SimType.ESIM).toString();

        assertThat(text).doesNotContain(VALID_IMSI).doesNotContain(VALID_ICCID);
        assertThat(Imsi.of(VALID_IMSI)).hasToString("99970**********");
        assertThat(Iccid.of(VALID_ICCID)).hasToString("***************0239");
    }

    @Nested
    @DisplayName("IMSI")
    class ImsiRules {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("refuse un IMSI absent")
        void rejectsMissingImsi(String imsi) {
            assertThatThrownBy(() -> Imsi.of(imsi))
                    .isInstanceOf(InvalidNetworkIdentityException.class)
                    .hasMessageContaining("obligatoire");
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "99970000001023",     // 14 chiffres
                "9997000000102310",   // 16 chiffres
                "99970000001023X",    // caractère non numérique
                "999 700000010231"    // espace
        })
        @DisplayName("refuse un IMSI mal formé")
        void rejectsMalformedImsi(String imsi) {
            assertThatThrownBy(() -> Imsi.of(imsi))
                    .isInstanceOf(InvalidNetworkIdentityException.class)
                    .hasMessageContaining("15 chiffres");
        }
    }

    @Nested
    @DisplayName("ICCID")
    class IccidRules {

        @Test
        @DisplayName("accepte un ICCID de 20 chiffres")
        void acceptsTwentyDigits() {
            assertThat(Iccid.of("89997000000000102302").value()).hasSize(20);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @DisplayName("refuse un ICCID absent")
        void rejectsMissingIccid(String iccid) {
            assertThatThrownBy(() -> Iccid.of(iccid))
                    .isInstanceOf(InvalidNetworkIdentityException.class)
                    .hasMessageContaining("obligatoire");
        }

        @ParameterizedTest
        @ValueSource(strings = {
                "899970000000001023",     // 18 chiffres
                "899970000000001023900",  // 21 chiffres
                "1999700000000010239",    // ne commence pas par 89
                "89997000000000102X9"     // caractère non numérique
        })
        @DisplayName("refuse un ICCID hors du format E.118")
        void rejectsMalformedIccid(String iccid) {
            assertThatThrownBy(() -> Iccid.of(iccid))
                    .isInstanceOf(InvalidNetworkIdentityException.class)
                    .hasMessageContaining("89");
        }

        @Test
        @DisplayName("refuse un ICCID dont le chiffre de contrôle de Luhn est faux")
        void rejectsWrongCheckDigit() {
            assertThatThrownBy(() -> Iccid.of("8999700000000010238"))
                    .isInstanceOf(InvalidNetworkIdentityException.class)
                    .hasMessageContaining("contrôle");
        }
    }
}
