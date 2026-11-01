package com.candronex.order.api;

import com.candronex.order.application.PlaceOrderCommand;
import com.candronex.order.application.PlaceOrderResult;
import com.candronex.order.application.PlaceOrderUseCase;
import com.candronex.platform.security.CurrentClientId;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * Adaptateur entrant d'Order Service. Le suivi d'une commande (UC-05) est hors périmètre : seule la
 * création est exposée.
 */
@RestController
@RequestMapping("/api/v1/service-orders")
class OrderController {

    static final String IDEMPOTENCY_HEADER = "Idempotency-Key";

    private final PlaceOrderUseCase orders;

    OrderController(PlaceOrderUseCase orders) {
        this.orders = orders;
    }

    @PostMapping
    ResponseEntity<OrderResponse> place(
            @CurrentClientId String clientId,
            @AuthenticationPrincipal Jwt token,
            @RequestHeader(IDEMPOTENCY_HEADER) String idempotencyKey,
            @Valid @RequestBody OrderRequest request) {

        PlaceOrderCommand command = new PlaceOrderCommand(
                clientId,
                token.getTokenValue(),
                idempotencyKey,
                request.orderLines().stream()
                        .map(line -> new PlaceOrderCommand.OrderLine(
                                line.droneId(), line.serviceSpecificationId()))
                        .toList());

        PlaceOrderResult result = orders.placeOrder(command);
        OrderResponse body = OrderResponse.from(result.order());

        if (!result.created()) {
            // Rejeu : la commande existait déjà, rien n'a été créé.
            return ResponseEntity.ok(body);
        }
        return ResponseEntity
                .created(URI.create("/api/v1/service-orders/" + body.orderId()))
                .body(body);
    }
}
