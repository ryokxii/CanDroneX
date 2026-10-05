package com.candronex.ordermanagement;

import com.candronex.accessmanagement.published.ClientDirectory;
import com.candronex.accessmanagement.published.UnknownClientException;
import com.candronex.droneregistration.published.DroneDirectory;
import com.candronex.droneregistration.published.DroneSummary;
import com.candronex.ordermanagement.application.OrderPlacement;
import com.candronex.ordermanagement.application.PlaceOrderCommand;
import com.candronex.ordermanagement.application.PlaceOrderResult;
import com.candronex.ordermanagement.application.PlaceOrderService;
import com.candronex.ordermanagement.domain.DroneNotFoundException;
import com.candronex.ordermanagement.domain.DuplicateOrderLineException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.candronex.servicecatalog.published.ServiceType.C2_URLLC;
import static com.candronex.servicecatalog.published.ServiceType.IMAGERY_EMBB;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** UC-04 — commander les services de connectivité d'un drone. */
class PlaceOrderServiceTest {

    private static final UUID CLIENT = UUID.randomUUID();
    private static final UUID OTHER_CLIENT = UUID.randomUUID();
    private static final UUID DRONE = UUID.randomUUID();
    private static final String KEY = "11111111-2222-3333-4444-555555555555";
    private static final Instant NOW = Instant.parse("2026-10-04T14:15:40Z");

    private InMemoryOrderRepository orders;
    private PlaceOrderService service;

    @BeforeEach
    void setUp() {
        orders = new InMemoryOrderRepository();

        // Le drone DRONE existe et appartient à CLIENT, et à lui seul.
        DroneDirectory drones = (droneId, clientId) ->
                DRONE.equals(droneId) && CLIENT.equals(clientId)
                        ? Optional.of(new DroneSummary(droneId, clientId))
                        : Optional.empty();

        ClientDirectory clients = clientId ->
                CLIENT.equals(clientId) || OTHER_CLIENT.equals(clientId);

        OrderPlacement placement = new OrderPlacement(
                orders, drones, clients, Clock.fixed(NOW, ZoneOffset.UTC));
        service = new PlaceOrderService(placement, orders);
    }

    private PlaceOrderCommand orderFor(UUID clientId, String key,
                                       PlaceOrderCommand.OrderLine... orderLines) {
        return new PlaceOrderCommand(clientId, key, List.of(orderLines));
    }

    @Test
    @DisplayName("accepte une commande valide pour un drone enregistré")
    void acceptsValidOrder() {
        PlaceOrderResult result = service.placeOrder(orderFor(CLIENT, KEY,
                new PlaceOrderCommand.OrderLine(DRONE, C2_URLLC),
                new PlaceOrderCommand.OrderLine(DRONE, IMAGERY_EMBB)));

        assertThat(result.created()).isTrue();
        assertThat(result.order().clientId()).isEqualTo(CLIENT);
        assertThat(result.order().status()).isEqualTo("RECEIVED");
        assertThat(result.order().placedAt()).isEqualTo(NOW);
        assertThat(result.order().orderLines())
                .extracting(line -> line.serviceType() + "/" + line.status())
                .containsExactly("C2_URLLC/RECEIVED", "IMAGERY_EMBB/RECEIVED");
        assertThat(orders.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("refuse une commande comportant deux fois le même couple drone et service")
    void rejectsDuplicateLine() {
        assertThatThrownBy(() -> service.placeOrder(orderFor(CLIENT, KEY,
                new PlaceOrderCommand.OrderLine(DRONE, C2_URLLC),
                new PlaceOrderCommand.OrderLine(DRONE, C2_URLLC))))
                .isInstanceOf(DuplicateOrderLineException.class);

        assertThat(orders.count()).isZero();
    }

    @Test
    @DisplayName("refuse une commande visant un drone inconnu")
    void rejectsUnknownDrone() {
        assertThatThrownBy(() -> service.placeOrder(orderFor(CLIENT, KEY,
                new PlaceOrderCommand.OrderLine(UUID.randomUUID(), C2_URLLC))))
                .isInstanceOf(DroneNotFoundException.class);

        assertThat(orders.count()).isZero();
    }

    @Test
    @DisplayName("refuse une commande visant le drone d'un autre client")
    void rejectsDroneOfAnotherClient() {
        // Drone d'un autre client : indiscernable d'un drone inconnu.
        assertThatThrownBy(() -> service.placeOrder(orderFor(OTHER_CLIENT, KEY,
                new PlaceOrderCommand.OrderLine(DRONE, C2_URLLC))))
                .isInstanceOf(DroneNotFoundException.class);

        assertThat(orders.count()).isZero();
    }

    @Test
    @DisplayName("refuse une commande sans ligne")
    void rejectsEmptyOrder() {
        assertThatThrownBy(() -> service.placeOrder(orderFor(CLIENT, KEY)))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(orders.count()).isZero();
    }

    @Test
    @DisplayName("refuse une commande sans clé d'idempotence")
    void rejectsMissingIdempotencyKey() {
        assertThatThrownBy(() -> service.placeOrder(orderFor(CLIENT, null,
                new PlaceOrderCommand.OrderLine(DRONE, C2_URLLC))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("refuse une commande d'un client inconnu")
    void rejectsUnknownClient() {
        assertThatThrownBy(() -> service.placeOrder(orderFor(UUID.randomUUID(), KEY,
                new PlaceOrderCommand.OrderLine(DRONE, C2_URLLC))))
                .isInstanceOf(UnknownClientException.class);
    }

    @Test
    @DisplayName("rejeu séquentiel : la même clé retourne la commande existante")
    void sequentialReplayReturnsSameOrder() {
        PlaceOrderCommand command = orderFor(CLIENT, KEY,
                new PlaceOrderCommand.OrderLine(DRONE, C2_URLLC));

        PlaceOrderResult first = service.placeOrder(command);
        PlaceOrderResult replay = service.placeOrder(command);

        assertThat(first.created()).isTrue();
        assertThat(replay.created()).isFalse();
        assertThat(replay.order().orderId()).isEqualTo(first.order().orderId());
        assertThat(orders.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("rejeu simultané : la contrainte d'unicité tranche, une seule commande existe")
    void concurrentReplayReturnsTheOrderCreatedByTheOtherRequest() {
        PlaceOrderCommand command = orderFor(CLIENT, KEY,
                new PlaceOrderCommand.OrderLine(DRONE, C2_URLLC));

        // La première requête a créé la commande.
        PlaceOrderResult first = service.placeOrder(command);

        // La seconde, concurrente, viole UNIQUE (client_id, idempotency_key).
        orders.failNextSaveAsConcurrentReplay();
        orders.hideNextLookup();
        PlaceOrderResult concurrent = service.placeOrder(command);

        assertThat(concurrent.created()).isFalse();
        assertThat(concurrent.order().orderId()).isEqualTo(first.order().orderId());
        assertThat(orders.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("une commande n'est consultable que par son client")
    void isolatesOrdersByClient() {
        PlaceOrderResult placed = service.placeOrder(orderFor(CLIENT, KEY,
                new PlaceOrderCommand.OrderLine(DRONE, C2_URLLC)));
        UUID orderId = placed.order().orderId();

        assertThat(service.findForClient(orderId, CLIENT)).isNotNull();
        assertThatThrownBy(() -> service.findForClient(orderId, OTHER_CLIENT))
                .isInstanceOf(java.util.NoSuchElementException.class);
    }
}
