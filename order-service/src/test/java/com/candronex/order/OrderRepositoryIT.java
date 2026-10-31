package com.candronex.order;

import com.candronex.order.application.DependencyUnavailableException;
import com.candronex.order.application.PlaceOrderCommand;
import com.candronex.order.application.PlaceOrderResult;
import com.candronex.order.application.PlaceOrderService;
import com.candronex.order.domain.IdempotencyKey;
import com.candronex.order.domain.IdempotencyKeyConflictException;
import com.candronex.order.domain.Order;
import com.candronex.order.domain.OrderLine;
import com.candronex.order.domain.OrderLineStatus;
import com.candronex.order.domain.OrderStatus;
import com.candronex.order.domain.port.DroneOwnershipPort;
import com.candronex.order.domain.port.OrderRepository;
import com.candronex.order.domain.port.ServiceActivationPort;
import com.candronex.order.domain.port.ServiceState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.candronex.order.CatalogFixtures.C2;
import static com.candronex.order.CatalogFixtures.IMAGERY;
import static com.candronex.order.CatalogFixtures.c2;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

/** Persistance d'Order Service, sur PostgreSQL réel, avec le catalogue amorcé par V1. */
class OrderRepositoryIT extends PostgresIntegrationTest {

    private static final String CLIENT = "inspectra-demo";
    private static final String KEY = "11111111-2222-3333-4444-555555555555";

    @Autowired
    private OrderRepository orders;

    @Autowired
    private PlaceOrderService placeOrder;

    // Drone Service est un autre processus : sa réponse est simulée.
    @MockitoBean
    private DroneOwnershipPort drones;

    @MockitoBean
    private ServiceActivationPort activations;

    @Autowired
    private JdbcTemplate jdbc;

    private UUID drone;

    @BeforeEach
    void reset() {
        jdbc.execute("TRUNCATE TABLE orders.service_orders CASCADE");
        drone = UUID.randomUUID();
        when(drones.isOwnedBy(any(), any())).thenReturn(true);
        // Slice Service injoignable par défaut : les lignes restent RECEIVED.
        when(activations.request(any(), any(), any()))
                .thenThrow(new DependencyUnavailableException("Slice Service indisponible", null));
    }

    private PlaceOrderCommand twoServices(String key) {
        return new PlaceOrderCommand(CLIENT, "jeton", key, List.of(
                new PlaceOrderCommand.OrderLine(drone, C2),
                new PlaceOrderCommand.OrderLine(drone, IMAGERY)));
    }

    private Order reload(String key) {
        return orders.findByClientIdAndIdempotencyKey(CLIENT, IdempotencyKey.of(key)).orElseThrow();
    }

    @Test
    @DisplayName("l'agrégat est écrit avec ses lignes et ses caractéristiques figées, dans l'ordre")
    void persistsAggregateWithFrozenCharacteristics() {
        placeOrder.placeOrder(twoServices(KEY));

        Order reloaded = reload(KEY);
        assertThat(reloaded.status()).isEqualTo(OrderStatus.RECEIVED);
        assertThat(reloaded.orderLines())
                .extracting(line -> line.service().serviceSpecificationId())
                .containsExactly(C2, IMAGERY);

        OrderLine c2Line = reloaded.orderLines().get(0);
        assertThat(c2Line.service().characteristics())
                .extracting(characteristic -> characteristic.name() + "=" + characteristic.value())
                .containsExactly("sst=2", "sd=000001", "dnn=c2", "fiveQi=7", "arp=2",
                        "ambrUplink=20 Mbps", "ambrDownlink=20 Mbps");
    }

    @Test
    @DisplayName("un changement d'état et la date d'achèvement survivent à l'aller-retour en base")
    void persistsLifecycleChanges() {
        placeOrder.placeOrder(twoServices(KEY));
        Order order = reload(KEY);
        Instant settled = Instant.parse("2026-10-04T15:00:00Z");

        UUID c2Line = order.orderLines().get(0).orderLineId();
        UUID imageryLine = order.orderLines().get(1).orderLineId();
        order.updateLineStatus(c2Line, OrderLineStatus.IN_PROGRESS, settled);
        order.updateLineStatus(c2Line, OrderLineStatus.COMPLETED, settled);
        order.updateLineStatus(imageryLine, OrderLineStatus.IN_PROGRESS, settled);
        order.updateLineStatus(imageryLine, OrderLineStatus.FAILED, settled);
        orders.save(order);

        Order reloaded = reload(KEY);
        assertThat(reloaded.status()).isEqualTo(OrderStatus.PARTIAL);
        assertThat(reloaded.completedAt()).isEqualTo(settled);
        assertThat(reloaded.orderLines())
                .extracting(OrderLine::status)
                .containsExactly(OrderLineStatus.COMPLETED, OrderLineStatus.FAILED);
    }

    @Test
    @DisplayName("la contrainte UNIQUE (client_id, idempotency_key) refuse un doublon")
    void enforcesIdempotencyKeyUniqueness() {
        placeOrder.placeOrder(twoServices(KEY));

        // Contourne le service : vérifie la contrainte UNIQUE de dernier recours.
        Order duplicate = Order.place(CLIENT, IdempotencyKey.of(KEY), Instant.now());
        duplicate.addLine(drone, c2());

        assertThatThrownBy(() -> orders.save(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM orders.service_orders", Integer.class)).isEqualTo(1);
    }

    @Test
    @DisplayName("rejouer la même clé et le même contenu retourne la même commande, sans rien créer")
    void replayReturnsTheSameOrderOnARealDatabase() {
        PlaceOrderResult first = placeOrder.placeOrder(twoServices(KEY));
        PlaceOrderResult replay = placeOrder.placeOrder(twoServices(KEY));

        assertThat(first.created()).isTrue();
        assertThat(replay.created()).isFalse();
        assertThat(replay.order().orderId()).isEqualTo(first.order().orderId());
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM orders.order_lines", Integer.class)).isEqualTo(2);
    }

    @Test
    @DisplayName("rejouer la même clé avec un autre contenu est refusé")
    void replayWithOtherContentIsRejected() {
        placeOrder.placeOrder(twoServices(KEY));

        assertThatThrownBy(() -> placeOrder.placeOrder(new PlaceOrderCommand(CLIENT, "jeton", KEY,
                List.of(new PlaceOrderCommand.OrderLine(drone, C2)))))
                .isInstanceOf(IdempotencyKeyConflictException.class);
    }

    @Test
    @DisplayName("deux clés différentes produisent deux commandes")
    void distinctKeysProduceDistinctOrders() {
        PlaceOrderResult first = placeOrder.placeOrder(twoServices(KEY));
        PlaceOrderResult second = placeOrder.placeOrder(twoServices("autre-clé"));

        assertThat(second.created()).isTrue();
        assertThat(second.order().orderId()).isNotEqualTo(first.order().orderId());
    }

    @Test
    @DisplayName("la contrainte CHECK refuse une ligne à l'état PARTIAL, réservé à la commande")
    void lineCannotBePartialInDatabase() {
        placeOrder.placeOrder(twoServices(KEY));

        assertThatThrownBy(() -> jdbc.update("UPDATE orders.order_lines SET status = 'PARTIAL'"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("supprimer une commande supprime ses lignes et leurs caractéristiques")
    void cascadesDeletionToLines() {
        PlaceOrderResult placed = placeOrder.placeOrder(twoServices(KEY));

        jdbc.update("DELETE FROM orders.service_orders WHERE order_id = ?", placed.order().orderId());

        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM orders.order_lines", Integer.class)).isZero();
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM orders.order_line_characteristics", Integer.class)).isZero();
    }

    @Test
    @DisplayName("activation acceptée par Slice Service : lignes et commande IN_PROGRESS en base")
    void acceptedActivationMovesLinesInProgress() {
        doReturn(ServiceState.PENDING).when(activations).request(any(), any(), any());

        PlaceOrderResult placed = placeOrder.placeOrder(twoServices(KEY));

        assertThat(placed.order().status()).isEqualTo("IN_PROGRESS");
        Order reloaded = reload(KEY);
        assertThat(reloaded.status()).isEqualTo(OrderStatus.IN_PROGRESS);
        assertThat(reloaded.orderLines()).extracting(OrderLine::status)
                .containsExactly(OrderLineStatus.IN_PROGRESS, OrderLineStatus.IN_PROGRESS);
    }
}
