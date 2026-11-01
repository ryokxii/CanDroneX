package com.candronex.order.api;

import com.candronex.order.application.DependencyUnavailableException;
import com.candronex.order.application.OrderView;
import com.candronex.order.application.PlaceOrderCommand;
import com.candronex.order.application.PlaceOrderResult;
import com.candronex.order.application.PlaceOrderUseCase;
import com.candronex.order.domain.IdempotencyKeyConflictException;
import com.candronex.order.infrastructure.security.SecurityConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Contrat REST d'UC-04 : codes, en-tête Idempotency-Key, format RFC 9457. */
@WebMvcTest(OrderController.class)
@Import(SecurityConfiguration.class)
class OrderControllerTest {

    private static final String BODY = """
            {"orderLines":[{"droneId":"d0000000-0000-4000-8000-000000000001","serviceSpecificationId":"C2_URLLC"}]}
            """;
    private static final UUID ORDER_ID = UUID.fromString("a0000000-0000-4000-8000-000000000001");
    private static final OrderView ORDER = new OrderView(
            ORDER_ID, "RECEIVED", Instant.parse("2026-10-01T14:05:00Z"), null, List.of());

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private PlaceOrderUseCase orders;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private static MockHttpServletRequestBuilder order(String idempotencyKey) {
        MockHttpServletRequestBuilder request = post("/api/v1/service-orders")
                .with(jwt().jwt(token -> token.tokenValue("jeton-inspectra").claim("azp", "inspectra-demo")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY);
        return idempotencyKey == null ? request : request.header("Idempotency-Key", idempotencyKey);
    }

    @Test
    @DisplayName("sans jeton : 401")
    void rejectsMissingToken() throws Exception {
        mvc.perform(post("/api/v1/service-orders").header("Idempotency-Key", "k")
                        .contentType(MediaType.APPLICATION_JSON).content(BODY))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(orders);
    }

    @Test
    @DisplayName("sans Idempotency-Key : 400")
    void rejectsMissingIdempotencyKey() throws Exception {
        mvc.perform(order(null))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://candronex.ca/problems/bad-request"));

        verifyNoInteractions(orders);
    }

    @Test
    @DisplayName("création : 201 ; le client et son jeton viennent du JWT")
    void createsOrder() throws Exception {
        when(orders.placeOrder(any())).thenReturn(new PlaceOrderResult(ORDER, true));

        mvc.perform(order("demo-commande-0001"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/service-orders/" + ORDER_ID))
                .andExpect(jsonPath("$.orderId").value(ORDER_ID.toString()));

        verify(orders).placeOrder(argThat((PlaceOrderCommand command) ->
                command.clientId().equals("inspectra-demo")
                        && command.clientToken().equals("jeton-inspectra")
                        && command.idempotencyKey().equals("demo-commande-0001")));
    }

    @Test
    @DisplayName("rejeu, même contenu : 200 ; autre contenu : 409")
    void replays() throws Exception {
        when(orders.placeOrder(any())).thenReturn(new PlaceOrderResult(ORDER, false));
        mvc.perform(order("demo-commande-0001")).andExpect(status().isOk());

        when(orders.placeOrder(any())).thenThrow(new IdempotencyKeyConflictException());
        mvc.perform(order("demo-commande-0001"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("https://candronex.ca/problems/idempotency-key-reused"));
    }

    @Test
    @DisplayName("Drone Service indisponible : 503 dependency-unavailable")
    void droneServiceUnavailable() throws Exception {
        when(orders.placeOrder(any())).thenThrow(new DependencyUnavailableException("arrêté", null));

        mvc.perform(order("demo-commande-0001"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.type").value("https://candronex.ca/problems/dependency-unavailable"))
                .andExpect(jsonPath("$.correlationId").exists());
    }
}
