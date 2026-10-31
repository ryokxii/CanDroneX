package com.candronex.order.application;

import com.candronex.order.catalog.application.BrowseCatalogUseCase;
import com.candronex.order.catalog.application.ServiceSpecificationSummary;
import com.candronex.order.domain.IdempotencyKey;
import com.candronex.order.domain.Order;
import com.candronex.order.domain.OrderedService;
import com.candronex.order.domain.ServiceCharacteristic;
import com.candronex.order.domain.UnknownServiceSpecificationException;
import com.candronex.order.domain.port.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Optional;

/** Le travail transactionnel d'UC-04, après la vérification des drones auprès de Drone Service. */
@Service
public class OrderPlacement {

    private final OrderRepository orders;
    private final BrowseCatalogUseCase catalog;
    private final Clock clock;

    public OrderPlacement(OrderRepository orders, BrowseCatalogUseCase catalog, Clock clock) {
        this.orders = orders;
        this.catalog = catalog;
        this.clock = clock;
    }

    /** Crée la commande, ou retourne celle qui porte déjà cette clé pour le même contenu. */
    @Transactional
    public PlaceOrderResult place(PlaceOrderCommand command, IdempotencyKey key) {
        Optional<Order> alreadyPlaced =
                orders.findByClientIdAndIdempotencyKey(command.clientId(), key);
        if (alreadyPlaced.isPresent()) {
            alreadyPlaced.get().verifyReplayOf(command.lineKeys());
            return PlaceOrderResult.replayed(OrderView.from(alreadyPlaced.get()));
        }

        Order order = Order.place(command.clientId(), key, clock.instant());
        for (PlaceOrderCommand.OrderLine requested : command.orderLines()) {
            order.addLine(requested.droneId(), freeze(requested.serviceSpecificationId()));
        }

        orders.save(order);
        return PlaceOrderResult.created(OrderView.from(order));
    }

    /** Copie l'offre courante du catalogue : la commande garde ce qu'elle a reçu. */
    private OrderedService freeze(String serviceSpecificationId) {
        ServiceSpecificationSummary specification = catalog.findSpecification(serviceSpecificationId)
                .orElseThrow(() -> new UnknownServiceSpecificationException(serviceSpecificationId));

        return new OrderedService(
                specification.serviceSpecificationId(),
                specification.characteristics().stream()
                        .map(characteristic -> new ServiceCharacteristic(
                                characteristic.name(), characteristic.value()))
                        .toList());
    }
}
