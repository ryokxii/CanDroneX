package com.candronex.accessmanagement.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Extraction des identifiants du client. */
class ClientAuthenticationTest {

    private static String basic(String id, String secret) {
        String raw = URLEncoder.encode(id, StandardCharsets.UTF_8) + ":"
                + URLEncoder.encode(secret, StandardCharsets.UTF_8);
        return "Basic " + Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("lit client_secret_basic, y compris un secret contenant ':' et des caractères encodés")
    void readsBasicHeader() {
        ClientAuthentication auth = ClientAuthentication.resolve(basic("CLI-A", "a:b c+é"), null, null);

        assertThat(auth.clientId()).isEqualTo("CLI-A");
        assertThat(auth.clientSecret()).isEqualTo("a:b c+é");
        assertThat(auth.viaBasic()).isTrue();
    }

    @Test
    @DisplayName("lit client_secret_post")
    void readsBodyParameters() {
        ClientAuthentication auth = ClientAuthentication.resolve(null, "CLI-A", "secret");

        assertThat(auth.clientId()).isEqualTo("CLI-A");
        assertThat(auth.clientSecret()).isEqualTo("secret");
        assertThat(auth.viaBasic()).isFalse();
    }

    @Test
    @DisplayName("refuse deux modes d'authentification à la fois")
    void rejectsBothMethods() {
        assertThatThrownBy(() -> ClientAuthentication.resolve(basic("CLI-A", "s"), "CLI-A", "s"))
                .isInstanceOfSatisfying(InvalidTokenRequestException.class,
                        e -> assertThat(e.error()).isEqualTo("invalid_request"));
    }

    @Test
    @DisplayName("refuse une requête sans identifiants")
    void rejectsMissingCredentials() {
        assertThatThrownBy(() -> ClientAuthentication.resolve(null, null, null))
                .isInstanceOfSatisfying(InvalidTokenRequestException.class,
                        e -> assertThat(e.error()).isEqualTo("invalid_client"));
    }

    @Test
    @DisplayName("refuse un en-tête Basic mal formé")
    void rejectsMalformedBasic() {
        String noColon = "Basic " + Base64.getEncoder().encodeToString("sansdeuxpoints".getBytes());
        String badEscape = "Basic " + Base64.getEncoder().encodeToString("CLI%ZZ:s".getBytes());

        for (String header : new String[] {"Basic !!!pas-base64", noColon, badEscape}) {
            assertThatThrownBy(() -> ClientAuthentication.resolve(header, null, null))
                    .isInstanceOfSatisfying(InvalidTokenRequestException.class,
                            e -> assertThat(e.error()).isEqualTo("invalid_request"));
        }
    }

    @Test
    @DisplayName("le secret n'apparaît pas dans la représentation textuelle")
    void doesNotLeakSecret() {
        assertThat(ClientAuthentication.resolve(null, "CLI-A", "tres-secret").toString())
                .doesNotContain("tres-secret");
    }
}
