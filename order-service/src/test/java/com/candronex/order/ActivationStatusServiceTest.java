package com.candronex.order;

import com.candronex.order.application.ActivationStatusService;
import com.candronex.order.application.DependencyUnavailableException;
import com.candronex.order.domain.IdempotencyKey;
import com.candronex.order.domain.Order;
import com.candronex.order.domain.OrderLine;
import com.candronex.order.domain.OrderLineStatus;
import com.candronex.order.domain.OrderStatus;
import com.candronex.order.domain.ServiceCharacteristic;
import com.candronex.order.domain.port.ServiceActivationPort;
import com.candronex.order.domain.port.ServiceState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.candronex.order.CatalogFixtures.c2;
import static com.candronex.order.CatalogFixtures.imagery;
import static org.assertj.core.api.Assertions.assertThat;

/** UC-06 vu d'Order Service : l'état des lignes suit celui des services, sans rappel. */
class ActivationStatusServiceTest {

    private static final UUID DRONE = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-10-01T14:05:03Z");

    private InMemoryOrderRepository orders;
    private FakeSliceService slices;
    private ActivationStatusService sync;
    private Order order;

    /** Slice Service simulé : un état par ligne, ou indisponible. */
    static final class FakeSliceService implements ServiceActivationPort {
        final Map<UUID, ServiceState> states = new HashMap<>();
        final List<UUID> requested = new ArrayList<>();
        boolean down;

        @Override
        public ServiceState request(UUID orderLineId, UUID droneId, List<ServiceCharacteristic> characteristics) {
            if (down) {
                throw new DependencyUnavailableException("Slice Service indisponible", null);
            }
            requested.add(orderLineId);
            return states.computeIfAbsent(orderLineId, id -> ServiceState.PENDING);
        }

        @Override
        public Optional<ServiceState> findByOrderLine(UUID orderLineId) {
            if (down) {
                throw new DependencyUnavailableException("Slice Service indisponible", null);
            }
            return Optional.ofNullable(states.get(orderLineId));
        }
    }

    /** Transactions sans effet : le test porte sur la logique, pas sur la base. */
    static final class NoTransactions extends AbstractPlatformTransactionManager {
        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
        }
    }

    @BeforeEach
    void setUp() {
        orders = new InMemoryOrderRepository();
        slices = new FakeSliceService();
        sync = new ActivationStatusService(orders, slices,
                new TransactionTemplate(new NoTransactions()), Clock.fixed(NOW, ZoneOffset.UTC), 200);
        order = Order.place("inspectra-demo", IdempotencyKey.of("demo-commande-0001"), NOW.minusSeconds(3));
        order.addLine(DRONE, c2());
        order.addLine(DRONE, imagery());
        orders.save(order);
    }

    private UUID line(int index) {
        return order.orderLines().get(index).orderLineId();
    }

    private List<OrderLineStatus> lineStatuses() {
        return order.orderLines().stream().map(OrderLine::status).toList();
    }

    @Test
    @DisplayName("lignes RECEIVED : activation demandée, lignes et commande IN_PROGRESS")
    void receivedLinesAreRequested() {
        sync.sync(order.orderId());

        assertThat(slices.requested).containsExactly(line(0), line(1));
        assertThat(lineStatuses()).containsOnly(OrderLineStatus.IN_PROGRESS);
        assertThat(order.status()).isEqualTo(OrderStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("C2 ACTIVE et Imagerie FAILED : commande PARTIAL, avec completedAt")
    void mixedResultsArePartial() {
        sync.sync(order.orderId());
        slices.states.put(line(0), ServiceState.ACTIVE);
        slices.states.put(line(1), ServiceState.FAILED);

        sync.sync(order.orderId());

        assertThat(lineStatuses()).containsExactly(OrderLineStatus.COMPLETED, OrderLineStatus.FAILED);
        assertThat(order.status()).isEqualTo(OrderStatus.PARTIAL);
        assertThat(order.completedAt()).isEqualTo(NOW);
        assertThat(orders.findPendingOrderIds(10)).isEmpty();
    }

    @Test
    @DisplayName("UNCONFIRMED n'est pas un échec : la ligne reste IN_PROGRESS")
    void unconfirmedStaysInProgress() {
        sync.sync(order.orderId());
        slices.states.put(line(0), ServiceState.UNCONFIRMED);
        slices.states.put(line(1), ServiceState.ACTIVE);

        sync.sync(order.orderId());

        assertThat(lineStatuses()).containsExactly(OrderLineStatus.IN_PROGRESS, OrderLineStatus.COMPLETED);
        assertThat(order.status()).isEqualTo(OrderStatus.IN_PROGRESS);
        assertThat(order.completedAt()).isNull();
    }

    @Test
    @DisplayName("Slice Service indisponible : rien ne change, les lignes attendent le prochain cycle")
    void unavailableSliceServiceChangesNothing() {
        slices.down = true;

        sync.sync(order.orderId());

        assertThat(lineStatuses()).containsOnly(OrderLineStatus.RECEIVED);
        assertThat(orders.findPendingOrderIds(10)).containsExactly(order.orderId());
    }

    @Test
    @DisplayName("ligne déjà activée lors d'une demande précédente : COMPLETED dès la reprise")
    void replayedRequestOfAnActiveServiceCompletesTheLine() {
        slices.states.put(line(0), ServiceState.ACTIVE);
        slices.states.put(line(1), ServiceState.ACTIVE);

        sync.sync(order.orderId());

        assertThat(lineStatuses()).containsOnly(OrderLineStatus.COMPLETED);
        assertThat(order.status()).isEqualTo(OrderStatus.COMPLETED);
    }
}
