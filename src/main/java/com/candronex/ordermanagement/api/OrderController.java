package com.candronex.ordermanagement.api;

import com.candronex.common.security.CurrentClientId;
import com.candronex.ordermanagement.application.PlaceOrderCommand;
import com.candronex.ordermanagement.application.PlaceOrderResult;
import com.candronex.ordermanagement.application.PlaceOrderService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

/** Adaptateur entrant du module OrderManagement. */
@RestController
@RequestMapping("/api/v1/service-orders")
class OrderController {

    static final String IDEMPOTENCY_HEADER = "Idempotency-Key";

    private final PlaceOrderService orders;

    OrderController(PlaceOrderService orders) {
        this.orders = orders;
    }

    @PostMapping
    ResponseEntity<OrderResponse> place(
            @CurrentClientId UUID clientId,
            @RequestHeader(IDEMPOTENCY_HEADER) String idempotencyKey,
            @Valid @RequestBody OrderRequest request) {

        PlaceOrderCommand command = new PlaceOrderCommand(
                clientId,
                idempotencyKey,
                request.orderLines().stream()
                        .map(line -> new PlaceOrderCommand.OrderLine(
                                line.droneId(), line.serviceType()))
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

    @GetMapping("/{orderId}")
    OrderResponse findMine(
            @CurrentClientId UUID clientId,
            @PathVariable UUID orderId) {

        return OrderResponse.from(orders.findForClient(orderId, clientId));
    }
}
