package com.candronex.accessmanagement;

import com.candronex.accessmanagement.domain.ApiCredential;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Cycle de vie d'un identifiant OAuth 2.0 : émis, expiré, révoqué. */
class ApiCredentialTest {

    private static final Instant ISSUED = Instant.parse("2026-10-01T00:00:00Z");
    private static final Instant EXPIRES = Instant.parse("2026-11-01T00:00:00Z");
    private static final String HASH = "$2a$10$empreinte";

    @Test
    @DisplayName("un identifiant sans échéance reste utilisable")
    void credentialWithoutExpiryIsUsable() {
        ApiCredential credential = ApiCredential.issue("CLI-A", HASH, ISSUED, null);

        assertThat(credential.isUsableAt(ISSUED.plusSeconds(10L * 365 * 24 * 3600))).isTrue();
    }

    @Test
    @DisplayName("un identifiant n'est plus utilisable à partir de son échéance")
    void credentialExpires() {
        ApiCredential credential = ApiCredential.issue("CLI-A", HASH, ISSUED, EXPIRES);

        assertThat(credential.isUsableAt(EXPIRES.minusSeconds(1))).isTrue();
        assertThat(credential.isUsableAt(EXPIRES)).isFalse();
    }

    @Test
    @DisplayName("la révocation retourne une copie, et l'original reste inchangé")
    void revocationIsImmutable() {
        ApiCredential original = ApiCredential.issue("CLI-A", HASH, ISSUED, null);
        Instant revocation = ISSUED.plusSeconds(60);

        ApiCredential revoked = original.revokedAt(revocation);

        assertThat(original.isUsableAt(revocation)).isTrue();
        assertThat(revoked.isUsableAt(revocation.minusSeconds(1))).isTrue();
        assertThat(revoked.isUsableAt(revocation)).isFalse();
        assertThat(revoked.revokedAt(revocation.plusSeconds(60)).isUsableAt(revocation)).isFalse();
    }

    @Test
    @DisplayName("refuse un identifiant sans client, sans empreinte ou à échéance incohérente")
    void rejectsInvalidCredential() {
        assertThatThrownBy(() -> ApiCredential.issue(" ", HASH, ISSUED, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ApiCredential.issue("CLI-A", null, ISSUED, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ApiCredential.issue("CLI-A", HASH, ISSUED, ISSUED))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
