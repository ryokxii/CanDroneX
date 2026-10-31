package com.candronex.order;

import com.candronex.order.application.DependencyUnavailableException;
import com.candronex.order.application.OrderPlacement;
import com.candronex.order.application.OrderView;
import com.candronex.order.application.PlaceOrderCommand;
import com.candronex.order.application.PlaceOrderResult;
import com.candronex.order.application.PlaceOrderService;
import com.candronex.order.application.SyncActivationStatusUseCase;
import com.candronex.order.domain.DroneNotFoundException;
import com.candronex.order.domain.DuplicateOrderLineException;
import com.candronex.order.domain.IdempotencyKeyConflictException;
import com.candronex.order.domain.UnknownServiceSpecificationException;
import com.candronex.order.domain.port.DroneOwnershipPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.candronex.order.CatalogFixtures.C2;
import static com.candronex.order.CatalogFixtures.IMAGERY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** UC-04 — commander les services de connectivité d'un drone. */
class PlaceOrderServiceTest {

    private static final String CLIENT = "inspectra-demo";
    private static final String OTHER_CLIENT = "aerotech-demo";
    private static final UUID DRONE = UUID.randomUUID();
    private static final String KEY = "11111111-2222-3333-4444-555555555555";
    private static final Instant NOW = Instant.parse("2026-10-04T14:15:40Z");

    private InMemoryOrderRepository orders;
    private CatalogFixtures.MutableCatalog catalog;
    private PlaceOrderService service;
    private final List<UUID> syncedOrders = new ArrayList<>();
    private boolean sliceServiceDown;

    /** Demande d'activation après la commande : seulement notée ici. */
    private final SyncActivationStatusUseCase activations = new SyncActivationStatusUseCase() {
        @Override
        public void sync(UUID orderId) {
            if (sliceServiceDown) {
                throw new IllegalStateException("Slice Service injoignable");
            }
            syncedOrders.add(orderId);
        }

        @Override
        public List<UUID> pendingOrders() {
            return List.of();
        }
    };

    @BeforeEach
    void setUp() {
        orders = new InMemoryOrderRepository();
        catalog = CatalogFixtures.catalog();

        // Le drone DRONE existe et appartient à CLIENT, et à lui seul ; le jeton porte le client.
        DroneOwnershipPort drones = (droneId, clientToken) ->
                DRONE.equals(droneId) && tokenOf(CLIENT).equals(clientToken);

        OrderPlacement placement = new OrderPlacement(
                orders, catalog, Clock.fixed(NOW, ZoneOffset.UTC));
        service = new PlaceOrderService(placement, orders, drones, activations);
    }

    private static String tokenOf(String clientId) {
        return "jeton-" + clientId;
    }

    private static PlaceOrderCommand orderFor(String clientId, String key,
                                              PlaceOrderCommand.OrderLine... orderLines) {
        return new PlaceOrderCommand(clientId, tokenOf(clientId), key, List.of(orderLines));
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

        catalog.changeC2Arp("1");
        PlaceOrderResult replay = service.placeOrder(orderFor(CLIENT, KEY, line(DRONE, C2)));
        PlaceOrderResult after = service.placeOrder(orderFor(CLIENT, "autre-clé", line(DRONE, C2)));

        assertThat(characteristicsOf(replay.order().orderLines().get(0))).contains("arp=2");
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
    @DisplayName("Drone Service indisponible : la commande est refusée et rien n'est écrit")
    void rejectsOrderWhenDroneServiceIsUnavailable() {
        PlaceOrderService unavailable = new PlaceOrderService(
                new OrderPlacement(orders, catalog, Clock.fixed(NOW, ZoneOffset.UTC)), orders,
                (droneId, clientToken) -> {
                    throw new DependencyUnavailableException("Drone Service indisponible", null);
                }, activations);

        assertThatThrownBy(() -> unavailable.placeOrder(orderFor(CLIENT, KEY, line(DRONE, C2))))
                .isInstanceOf(DependencyUnavailableException.class);

        assertThat(orders.count()).isZero();
    }

    @Test
    @DisplayName("une commande créée déclenche la demande d'activation de ses lignes")
    void createdOrderRequestsActivations() {
        PlaceOrderResult result = service.placeOrder(orderFor(CLIENT, KEY, line(DRONE, C2)));

        assertThat(syncedOrders).containsExactly(result.order().orderId());
    }

    @Test
    @DisplayName("Slice Service injoignable : la commande reste créée, RECEIVED, pour le poller")
    void orderSurvivesActivationFailure() {
        sliceServiceDown = true;

        PlaceOrderResult result = service.placeOrder(orderFor(CLIENT, KEY, line(DRONE, C2)));

        assertThat(result.created()).isTrue();
        assertThat(result.order().status()).isEqualTo("RECEIVED");
        assertThat(orders.count()).isEqualTo(1);
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
        @DisplayName("un rejeu est servi sans appeler Drone Service, même indisponible")
        void replayDoesNotCallDroneService() {
            PlaceOrderResult first = service.placeOrder(orderFor(CLIENT, KEY, line(DRONE, C2)));
            PlaceOrderService droneServiceDown = new PlaceOrderService(
                    new OrderPlacement(orders, catalog, Clock.fixed(NOW, ZoneOffset.UTC)), orders,
                    (droneId, clientToken) -> {
                        throw new DependencyUnavailableException("Drone Service indisponible", null);
                    }, activations);

            PlaceOrderResult replay = droneServiceDown.placeOrder(orderFor(CLIENT, KEY, line(DRONE, C2)));

            assertThat(replay.created()).isFalse();
            assertThat(replay.order().orderId()).isEqualTo(first.order().orderId());
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
            orders.hideLookupsBeforeSave();
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
            orders.hideLookupsBeforeSave();

            assertThatThrownBy(() -> service.placeOrder(orderFor(CLIENT, KEY, line(DRONE, IMAGERY))))
                    .isInstanceOf(IdempotencyKeyConflictException.class);
            assertThat(orders.count()).isEqualTo(1);
        }
    }
}
