package com.candronex.ordermanagement;

import com.candronex.PostgresIntegrationTest;
import com.candronex.dronemanagement.application.RegisterDroneCommand;
import com.candronex.dronemanagement.application.RegisterDroneService;
import com.candronex.dronemanagement.domain.SimType;
import com.candronex.ordermanagement.application.PlaceOrderCommand;
import com.candronex.ordermanagement.application.PlaceOrderResult;
import com.candronex.ordermanagement.application.PlaceOrderService;
import com.candronex.ordermanagement.domain.IdempotencyKey;
import com.candronex.ordermanagement.domain.Order;
import com.candronex.ordermanagement.domain.OrderLine;
import com.candronex.ordermanagement.domain.OrderLineStatus;
import com.candronex.ordermanagement.domain.OrderStatus;
import com.candronex.ordermanagement.domain.port.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static com.candronex.ordermanagement.CatalogFixtures.C2;
import static com.candronex.ordermanagement.CatalogFixtures.IMAGERY;
import static com.candronex.ordermanagement.CatalogFixtures.c2;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Persistance du module OrderManagement, sur PostgreSQL réel, avec le catalogue amorcé par V8. */
class OrderRepositoryIT extends PostgresIntegrationTest {

    private static final UUID CLIENT = DEMO_CLIENT;
    private static final String KEY = "11111111-2222-3333-4444-555555555555";

    @Autowired
    private OrderRepository orders;

    @Autowired
    private PlaceOrderService placeOrder;

    @Autowired
    private RegisterDroneService registerDrone;

    @Autowired
    private JdbcTemplate jdbc;

    private UUID drone;

    @BeforeEach
    void reset() {
        jdbc.execute("TRUNCATE TABLE orders.service_orders CASCADE");
        jdbc.execute("TRUNCATE TABLE drone.drones");
        drone = registerDrone.register(new RegisterDroneCommand(
                CLIENT, "999700000010231", "8999700000000010239", SimType.ESIM)).droneId();
    }

    private PlaceOrderCommand twoServices(String key) {
        return new PlaceOrderCommand(CLIENT, key, List.of(
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
        assertThat(c2Line.service().serviceSpecificationVersion()).isEqualTo(1);
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
}
