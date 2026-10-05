package com.candronex.ordermanagement;

import com.candronex.PostgresIntegrationTest;
import com.candronex.droneregistration.application.RegisterDroneCommand;
import com.candronex.droneregistration.application.RegisterDroneService;
import com.candronex.droneregistration.domain.SimType;
import com.candronex.ordermanagement.application.PlaceOrderCommand;
import com.candronex.ordermanagement.application.PlaceOrderResult;
import com.candronex.ordermanagement.application.PlaceOrderService;
import com.candronex.ordermanagement.domain.IdempotencyKey;
import com.candronex.ordermanagement.domain.Order;
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

import static com.candronex.servicecatalog.published.ServiceType.C2_URLLC;
import static com.candronex.servicecatalog.published.ServiceType.IMAGERY_EMBB;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Persistance du module OrderManagement, sur PostgreSQL réel. */
class OrderRepositoryIT extends PostgresIntegrationTest {

    private static final UUID CLIENT = DEMO_CLIENT;
    private static final String IMSI = "302720123456789";
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
        drone = registerDrone.register(
                new RegisterDroneCommand(CLIENT, IMSI, SimType.ESIM)).droneId();
    }

    private PlaceOrderCommand twoServices(String key) {
        return new PlaceOrderCommand(CLIENT, key, List.of(
                new PlaceOrderCommand.OrderLine(drone, C2_URLLC),
                new PlaceOrderCommand.OrderLine(drone, IMAGERY_EMBB)));
    }

    @Test
    @DisplayName("l'agrégat est écrit avec ses lignes, par cascade")
    void persistsAggregateWithItsLines() {
        PlaceOrderResult placed = placeOrder.placeOrder(twoServices(KEY));
        UUID orderId = placed.order().orderId();

        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM orders.order_lines WHERE order_id = ?",
                Integer.class, orderId)).isEqualTo(2);

        Order reloaded = orders.findByIdAndClientId(orderId, CLIENT).orElseThrow();
        assertThat(reloaded.orderLines()).hasSize(2);
        assertThat(reloaded.status()).isEqualTo(OrderStatus.RECEIVED);
        assertThat(reloaded.idempotencyKey()).isEqualTo(IdempotencyKey.of(KEY));
    }

    @Test
    @DisplayName("la contrainte UNIQUE (client_id, idempotency_key) refuse un doublon")
    void enforcesIdempotencyKeyUniqueness() {
        placeOrder.placeOrder(twoServices(KEY));

        // Contourne le service : vérifie la contrainte UNIQUE de dernier recours.
        Order duplicate = Order.place(CLIENT, IdempotencyKey.of(KEY), Instant.now());
        duplicate.addLine(drone, C2_URLLC);

        assertThatThrownBy(() -> orders.save(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM orders.service_orders", Integer.class)).isEqualTo(1);
    }

    @Test
    @DisplayName("rejouer la même clé retourne la même commande, sans rien créer")
    void replayReturnsTheSameOrderOnARealDatabase() {
        PlaceOrderResult first = placeOrder.placeOrder(twoServices(KEY));
        PlaceOrderResult replay = placeOrder.placeOrder(twoServices(KEY));

        assertThat(first.created()).isTrue();
        assertThat(replay.created()).isFalse();
        assertThat(replay.order().orderId()).isEqualTo(first.order().orderId());

        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM orders.service_orders", Integer.class)).isEqualTo(1);
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
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM orders.service_orders", Integer.class)).isEqualTo(2);
    }

    @Test
    @DisplayName("la contrainte CHECK refuse un type de service hors de l'offre")
    void enforcesServiceTypeInDatabase() {
        PlaceOrderResult placed = placeOrder.placeOrder(twoServices(KEY));

        assertThatThrownBy(() -> jdbc.update(
                """
                INSERT INTO orders.order_lines
                    (order_line_id, order_id, drone_id, service_type, status)
                VALUES (?, ?, ?, 'VOICE_5G', 'RECEIVED')
                """, UUID.randomUUID(), placed.order().orderId(), drone))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("supprimer une commande supprime ses lignes")
    void cascadesDeletionToLines() {
        PlaceOrderResult placed = placeOrder.placeOrder(twoServices(KEY));

        jdbc.update("DELETE FROM orders.service_orders WHERE order_id = ?",
                placed.order().orderId());

        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM orders.order_lines", Integer.class)).isZero();
    }
}
