package com.candronex.drone.api;

import com.candronex.drone.application.DroneView;
import com.candronex.drone.application.RegisterDroneCommand;
import com.candronex.drone.application.RegisterDroneService;
import com.candronex.drone.domain.DroneNotFoundException;
import com.candronex.drone.domain.InvalidNetworkIdentityException;
import com.candronex.drone.infrastructure.security.SecurityConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Contrat REST d'UC-02 : codes, format RFC 9457 et identité tirée du jeton. */
@WebMvcTest(DroneController.class)
@Import(SecurityConfiguration.class)
class DroneControllerTest {

    private static final String BODY = """
            {"imsi":"302720123456789","iccid":"8999700000000010239","simType":"ESIM"}
            """;
    private static final UUID DRONE_ID = UUID.fromString("d0000000-0000-4000-8000-000000000001");

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private RegisterDroneService drones;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private static RequestPostProcessor client(String azp) {
        return jwt().jwt(token -> token.claim("azp", azp));
    }

    @Test
    @DisplayName("sans jeton : 401 en problem+json, sans toucher au service")
    void rejectsMissingToken() throws Exception {
        mvc.perform(get("/api/v1/drones"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().exists("WWW-Authenticate"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("https://candronex.ca/problems/unauthorized"))
                .andExpect(jsonPath("$.correlationId").exists());

        verifyNoInteractions(drones);
    }

    @Test
    @DisplayName("enregistre pour le client du jeton, et non pour celui de X-Client-Id")
    void registersForTheTokenClient() throws Exception {
        when(drones.register(any())).thenReturn(
                new DroneView(DRONE_ID, "REGISTERED", Instant.parse("2026-10-01T13:00:00Z")));

        mvc.perform(post("/api/v1/drones").with(client("inspectra-demo"))
                        .header("X-Client-Id", "aerotech-demo")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/drones/" + DRONE_ID))
                .andExpect(jsonPath("$.droneId").value(DRONE_ID.toString()))
                .andExpect(jsonPath("$.imsi").doesNotExist())
                .andExpect(jsonPath("$.iccid").doesNotExist());

        verify(drones).register(argThat((RegisterDroneCommand command) ->
                command.clientId().equals("inspectra-demo")));
    }

    @Test
    @DisplayName("le drone d'un autre client : 404, indiscernable d'un drone inexistant")
    void hidesAnotherClientsDrone() throws Exception {
        when(drones.findForClient(eq(DRONE_ID), eq("aerotech-demo")))
                .thenThrow(new DroneNotFoundException(DRONE_ID));

        mvc.perform(get("/api/v1/drones/" + DRONE_ID).with(client("aerotech-demo")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("https://candronex.ca/problems/resource-not-found"));
    }

    @Test
    @DisplayName("identité réseau refusée par le domaine : 422")
    void rejectsInvalidNetworkIdentity() throws Exception {
        when(drones.register(any())).thenThrow(
                new InvalidNetworkIdentityException("L'IMSI doit comporter exactement 15 chiffres."));

        mvc.perform(post("/api/v1/drones").with(client("inspectra-demo"))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.type").value("https://candronex.ca/problems/invalid-network-identity"));
    }

    @Test
    @DisplayName("champ obligatoire absent : 400 ; corps hors JSON : 415")
    void rejectsMalformedRequests() throws Exception {
        mvc.perform(post("/api/v1/drones").with(client("inspectra-demo"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"simType\":\"SIM\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://candronex.ca/problems/bad-request"));

        mvc.perform(post("/api/v1/drones").with(client("inspectra-demo"))
                        .contentType(MediaType.TEXT_PLAIN).content(BODY))
                .andExpect(status().isUnsupportedMediaType());

        verifyNoInteractions(drones);
    }
}
