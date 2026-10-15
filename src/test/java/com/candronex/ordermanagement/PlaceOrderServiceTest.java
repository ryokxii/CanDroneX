package com.candronex.ordermanagement;

import com.candronex.customermanagement.published.ClientDirectory;
import com.candronex.customermanagement.published.UnknownClientException;
import com.candronex.dronemanagement.published.DroneDirectory;
import com.candronex.dronemanagement.published.DroneSummary;
import com.candronex.ordermanagement.application.OrderPlacement;
import com.candronex.ordermanagement.application.OrderView;
import com.candronex.ordermanagement.application.PlaceOrderCommand;
import com.candronex.ordermanagement.application.PlaceOrderResult;
import com.candronex.ordermanagement.application.PlaceOrderService;
import com.candronex.ordermanagement.domain.DroneNotFoundException;
import com.candronex.ordermanagement.domain.DuplicateOrderLineException;
import com.candronex.ordermanagement.domain.IdempotencyKeyConflictException;
import com.candronex.ordermanagement.domain.UnknownServiceSpecificationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.candronex.ordermanagement.CatalogFixtures.C2;
import static com.candronex.ordermanagement.CatalogFixtures.IMAGERY;
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
    private CatalogFixtures.MutableCatalog catalog;
    private PlaceOrderService service;

    @BeforeEach
    void setUp() {
        orders = new InMemoryOrderRepository();
        catalog = CatalogFixtures.catalog();

        // Le drone DRONE existe et appartient à CLIENT, et à lui seul.
        DroneDirectory drones = (droneId, clientId) ->
                DRONE.equals(droneId) && CLIENT.equals(clientId)
                        ? Optional.of(new DroneSummary(droneId, clientId))
                        : Optional.empty();

        ClientDirectory clients = clientId ->
                CLIENT.equals(clientId) || OTHER_CLIENT.equals(clientId);

        OrderPlacement placement = new OrderPlacement(
                orders, drones, clients, catalog, Clock.fixed(NOW, ZoneOffset.UTC));
        service = new PlaceOrderService(placement, orders);
    }

    private static PlaceOrderCommand orderFor(UUID clientId, String key,
                                              PlaceOrderCommand.OrderLine... orderLines) {
        return new PlaceOrderCommand(clientId, key, List.of(orderLines));
    }

    private static PlaceOrderCommand.OrderLine line(UUID droneId, String serviceSpecificationId) {
        return new PlaceOrderCommand.OrderLine(droneId, serviceSpecificationId);
    }

    private static List<String> characteristicsOf(OrderView.OrderLineView line) {
        return line.serviceCharacteristics().stream()
                .map(characteristic -> characteristic.name() + "=" + characteristic.value())
                .toList();
    }

    @Test
    @DisplayName("accepte une commande valide et fige les caractéristiques du catalogue")
    void acceptsValidOrderAndFreezesCharacteristics() {
        PlaceOrderResult result = service.placeOrder(orderFor(CLIENT, KEY,
                line(DRONE, C2), line(DRONE, IMAGERY)));

        assertThat(result.created()).isTrue();
        assertThat(result.order().status()).isEqualTo("RECEIVED");
        assertThat(result.order().placedAt()).isEqualTo(NOW);
        assertThat(result.order().completedAt()).isNull();
        assertThat(result.order().orderLines())
                .extracting(line -> line.serviceSpecificationId() + "/" + line.status())
                .containsExactly("C2_URLLC/RECEIVED", "IMAGERY_EMBB/RECEIVED");
        assertThat(characteristicsOf(result.order().orderLines().get(0)))
                .containsExactly("sst=2", "sd=000001", "dnn=c2", "fiveQi=7", "arp=2",
                        "ambrUplink=20 Mbps", "ambrDownlink=20 Mbps");
        assertThat(orders.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("une évolution du catalogue ne réécrit pas une commande déjà passée")
    void catalogChangeDoesNotRewriteAPlacedOrder() {
        PlaceOrderResult before = service.placeOrder(orderFor(CLIENT, KEY, line(DRONE, C2)));

        catalog.publishNewC2Version("1");
        PlaceOrderResult replay = service.placeOrder(orderFor(CLIENT, KEY, line(DRONE, C2)));
        PlaceOrderResult after = service.placeOrder(orderFor(CLIENT, "autre-clé", line(DRONE, C2)));

        assertThat(replay.order().orderLines().get(0).serviceSpecificationVersion()).isEqualTo(1);
        assertThat(characteristicsOf(replay.order().orderLines().get(0))).contains("arp=2");
        assertThat(after.order().orderLines().get(0).serviceSpecificationVersion()).isEqualTo(2);
        assertThat(characteristicsOf(after.order().orderLines().get(0))).contains("arp=1");
        assertThat(before.order().orderId()).isEqualTo(replay.order().orderId());
    }

    @Test
    @DisplayName("refuse un service qui ne figure pas au catalogue")
    void rejectsUnknownServiceSpecification() {
        assertThatThrownBy(() -> service.placeOrder(orderFor(CLIENT, KEY, line(DRONE, "VOICE_5G"))))
                .isInstanceOf(UnknownServiceSpecificationException.class);

        assertThat(orders.count()).isZero();
    }

    @Test
    @DisplayName("refuse une commande comportant deux fois le même couple drone et service")
    void rejectsDuplicateLine() {
        assertThatThrownBy(() -> service.placeOrder(orderFor(CLIENT, KEY,
                line(DRONE, C2), line(DRONE, C2))))
                .isInstanceOf(DuplicateOrderLineException.class);

        assertThat(orders.count()).isZero();
    }

    @Test
    @DisplayName("refuse une commande visant un drone inconnu")
    void rejectsUnknownDrone() {
        assertThatThrownBy(() -> service.placeOrder(orderFor(CLIENT, KEY,
                line(UUID.randomUUID(), C2))))
                .isInstanceOf(DroneNotFoundException.class);

        assertThat(orders.count()).isZero();
    }

    @Test
    @DisplayName("refuse une commande visant le drone d'un autre client, comme un drone inconnu")
    void rejectsDroneOfAnotherClient() {
        assertThatThrownBy(() -> service.placeOrder(orderFor(OTHER_CLIENT, KEY, line(DRONE, C2))))
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
        assertThatThrownBy(() -> service.placeOrder(orderFor(CLIENT, null, line(DRONE, C2))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("refuse une commande d'un client inconnu")
    void rejectsUnknownClient() {
        assertThatThrownBy(() -> service.placeOrder(orderFor(UUID.randomUUID(), KEY, line(DRONE, C2))))
                .isInstanceOf(UnknownClientException.class);
    }

    @Nested
    @DisplayName("idempotence")
    class Idempotence {

        @Test
        @DisplayName("rejeu séquentiel, même contenu : la commande existante est retournée")
        void sequentialReplayReturnsSameOrder() {
            PlaceOrderCommand command = orderFor(CLIENT, KEY, line(DRONE, C2), line(DRONE, IMAGERY));

            PlaceOrderResult first = service.placeOrder(command);
            PlaceOrderResult replay = service.placeOrder(
                    orderFor(CLIENT, KEY, line(DRONE, IMAGERY), line(DRONE, C2)));

            assertThat(first.created()).isTrue();
            assertThat(replay.created()).isFalse();
            assertThat(replay.order().orderId()).isEqualTo(first.order().orderId());
            assertThat(orders.count()).isEqualTo(1);
        }

        @Test
        @DisplayName("rejeu séquentiel, contenu différent : conflit, rien n'est créé")
        void sequentialReplayWithOtherContentIsAConflict() {
            service.placeOrder(orderFor(CLIENT, KEY, line(DRONE, C2)));

            assertThatThrownBy(() -> service.placeOrder(orderFor(CLIENT, KEY, line(DRONE, IMAGERY))))
                    .isInstanceOf(IdempotencyKeyConflictException.class);

            assertThat(orders.count()).isEqualTo(1);
        }

        @Test
        @DisplayName("la portée de la clé est le client : la même clé d'un autre client est indépendante")
        void keyIsScopedByClient() {
            service.placeOrder(orderFor(CLIENT, KEY, line(DRONE, C2)));

            // L'autre client ne possède pas DRONE : sa demande échoue sur le drone, pas sur la clé.
            assertThatThrownBy(() -> service.placeOrder(orderFor(OTHER_CLIENT, KEY, line(DRONE, C2))))
                    .isInstanceOf(DroneNotFoundException.class);
        }

        @Test
        @DisplayName("rejeu simultané : la contrainte d'unicité tranche, une seule commande existe")
        void concurrentReplayReturnsTheOrderCreatedByTheOtherRequest() {
            PlaceOrderCommand command = orderFor(CLIENT, KEY, line(DRONE, C2));
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
        @DisplayName("rejeu simultané, contenu différent : conflit après relecture")
        void concurrentReplayWithOtherContentIsAConflict() {
            service.placeOrder(orderFor(CLIENT, KEY, line(DRONE, C2)));

            orders.failNextSaveAsConcurrentReplay();
            orders.hideNextLookup();

            assertThatThrownBy(() -> service.placeOrder(orderFor(CLIENT, KEY, line(DRONE, IMAGERY))))
                    .isInstanceOf(IdempotencyKeyConflictException.class);
            assertThat(orders.count()).isEqualTo(1);
        }
    }
}
