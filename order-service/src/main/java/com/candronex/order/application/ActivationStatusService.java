package com.candronex.order.application;

import com.candronex.order.domain.Order;
import com.candronex.order.domain.OrderLine;
import com.candronex.order.domain.OrderLineStatus;
import com.candronex.order.domain.port.OrderRepository;
import com.candronex.order.domain.port.ServiceActivationPort;
import com.candronex.order.domain.port.ServiceState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Réalise SyncActivationStatusUseCase. Les appels à Slice Service se font hors transaction ; le
 * premier qui échoue arrête les suivants, et les lignes restantes attendent le prochain cycle.
 */
@Service
public class ActivationStatusService implements SyncActivationStatusUseCase {

    private static final Logger log = LoggerFactory.getLogger(ActivationStatusService.class);

    private final OrderRepository orders;
    private final ServiceActivationPort activations;
    private final TransactionTemplate transaction;
    private final Clock clock;
    private final int batchSize;

    public ActivationStatusService(OrderRepository orders, ServiceActivationPort activations,
                                   TransactionTemplate transaction, Clock clock,
                                   @Value("${candronex.activation.poll-batch}") int batchSize) {
        this.orders = orders;
        this.activations = activations;
        this.transaction = transaction;
        this.clock = clock;
        this.batchSize = batchSize;
    }

    @Override
    public void sync(UUID orderId) {
        Optional<Order> found = transaction.execute(status -> orders.findByIdForUpdate(orderId));
        if (found == null || found.isEmpty()) {
            return;
        }
        Map<UUID, ServiceState> observed = observe(found.get());
        if (!observed.isEmpty()) {
            transaction.executeWithoutResult(status -> apply(orderId, observed));
        }
    }

    @Override
    public List<UUID> pendingOrders() {
        return transaction.execute(status -> orders.findPendingOrderIds(batchSize));
    }

    /** L'état des services, ligne par ligne, tel que Slice Service le connaît. */
    private Map<UUID, ServiceState> observe(Order order) {
        Map<UUID, ServiceState> observed = new LinkedHashMap<>();
        try {
            for (OrderLine line : order.orderLines()) {
                if (line.status() == OrderLineStatus.RECEIVED) {
                    // Idempotent : une demande déjà reçue renvoie le service existant.
                    ServiceState state = activations.request(
                            line.orderLineId(), line.droneId(), line.service().characteristics());
                    log.info("event=activation-requested orderId={} orderLineId={} state={}",
                            order.orderId(), line.orderLineId(), state);
                    observed.put(line.orderLineId(), state);
                } else if (line.status() == OrderLineStatus.IN_PROGRESS) {
                    activations.findByOrderLine(line.orderLineId())
                            .ifPresent(state -> observed.put(line.orderLineId(), state));
                }
            }
        } catch (DependencyUnavailableException unavailable) {
            log.warn("event=activation-requested orderId={} error=dependency-unavailable "
                    + "detail=\"lignes restantes reprises au prochain cycle\"", order.orderId());
        }
        return observed;
    }

    /** Relue sous verrou : seules les transitions encore valides sont appliquées. */
    private void apply(UUID orderId, Map<UUID, ServiceState> observed) {
        Order order = orders.findByIdForUpdate(orderId).orElseThrow();
        List<OrderLineStatus> before = statuses(order);

        for (OrderLine line : order.orderLines()) {
            ServiceState state = observed.get(line.orderLineId());
            if (state == null) {
                continue;
            }
            if (line.status() == OrderLineStatus.RECEIVED) {
                order.updateLineStatus(line.orderLineId(), OrderLineStatus.IN_PROGRESS, clock.instant());
            }
            OrderLineStatus target = state.lineStatus();
            if (line.status() != target && line.status().canTransitionTo(target)) {
                order.updateLineStatus(line.orderLineId(), target, clock.instant());
            }
        }

        if (!before.equals(statuses(order))) {
            orders.save(order);
            log.info("event=order-status orderId={} lines={} state={}",
                    orderId, statuses(order), order.status());
        }
    }

    private static List<OrderLineStatus> statuses(Order order) {
        return order.orderLines().stream().map(OrderLine::status).toList();
    }
}
