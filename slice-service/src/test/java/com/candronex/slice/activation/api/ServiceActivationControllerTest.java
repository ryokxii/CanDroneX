package com.candronex.slice.activation.api;

import com.candronex.slice.activation.application.ActivateServiceUseCase;
import com.candronex.slice.activation.application.FindActivationUseCase;
import com.candronex.slice.activation.application.RequestActivationResult;
import com.candronex.slice.activation.application.ServiceView;
import com.candronex.slice.activation.domain.ServiceNotFoundException;
import com.candronex.slice.infrastructure.security.SecurityConfiguration;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Contrat REST interne de Slice Service : réservé à Order Service, 202 puis 200 au rejeu. */
@WebMvcTest(ServiceActivationController.class)
@Import(SecurityConfiguration.class)
class ServiceActivationControllerTest {

    private static final UUID ORDER_LINE = UUID.fromString("b0000000-0000-4000-8000-000000000001");
    private static final String BODY = """
            {"orderLineId":"%s","droneId":"d0000000-0000-4000-8000-000000000001",
             "characteristics":[{"name":"sst","value":"2"}]}
            """.formatted(ORDER_LINE);
    private static final ServiceView PENDING = new ServiceView(
            UUID.fromString("c0000000-0000-4000-8000-000000000001"), ORDER_LINE,
            UUID.fromString("d0000000-0000-4000-8000-000000000001"), "PENDING",
            Instant.parse("2026-10-01T14:05:00Z"), null);

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ActivateServiceUseCase activation;

    @MockitoBean
    private FindActivationUseCase activations;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private static RequestPostProcessor caller(String azp) {
        return jwt().jwt(token -> token.claim("azp", azp));
    }

    @Test
    @DisplayName("jeton d'un client B2B : 403, sans toucher au service")
    void rejectsB2bClient() throws Exception {
        mvc.perform(post("/internal/v1/service-activations").with(caller("inspectra-demo"))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("https://candronex.ca/problems/forbidden"));

        verifyNoInteractions(activation);
    }

    @Test
    @DisplayName("première demande d'Order Service : 202 et Location ; rejeu : 200")
    void acceptsThenReplays() throws Exception {
        when(activation.request(any())).thenReturn(new RequestActivationResult(PENDING, true));
        mvc.perform(post("/internal/v1/service-activations").with(caller("order-service"))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isAccepted())
                .andExpect(header().string("Location",
                        "/internal/v1/service-activations?orderLineId=" + ORDER_LINE))
                .andExpect(jsonPath("$.state").value("PENDING"));

        when(activation.request(any())).thenReturn(new RequestActivationResult(PENDING, false));
        mvc.perform(post("/internal/v1/service-activations").with(caller("order-service"))
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("consultation : 200 ; ligne inconnue : 404 ; orderLineId absent : 400")
    void findsByOrderLine() throws Exception {
        when(activations.findByOrderLine(ORDER_LINE)).thenReturn(PENDING);
        mvc.perform(get("/internal/v1/service-activations").param("orderLineId", ORDER_LINE.toString())
                        .with(caller("order-service")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceId").value(PENDING.serviceId().toString()));

        UUID unknown = UUID.randomUUID();
        when(activations.findByOrderLine(unknown)).thenThrow(new ServiceNotFoundException(unknown));
        mvc.perform(get("/internal/v1/service-activations").param("orderLineId", unknown.toString())
                        .with(caller("order-service")))
                .andExpect(status().isNotFound());

        mvc.perform(get("/internal/v1/service-activations").with(caller("order-service")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("caractéristiques absentes : 400")
    void rejectsMissingCharacteristics() throws Exception {
        mvc.perform(post("/internal/v1/service-activations").with(caller("order-service"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderLineId\":\"%s\",\"droneId\":\"%s\",\"characteristics\":[]}"
                                .formatted(ORDER_LINE, UUID.randomUUID())))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(activation);
    }
}
