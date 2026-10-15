package com.candronex.ordermanagement.application;

import com.candronex.customermanagement.published.ClientDirectory;
import com.candronex.customermanagement.published.UnknownClientException;
import com.candronex.dronemanagement.published.DroneDirectory;
import com.candronex.ordermanagement.domain.DroneNotFoundException;
import com.candronex.ordermanagement.domain.IdempotencyKey;
import com.candronex.ordermanagement.domain.Order;
import com.candronex.ordermanagement.domain.OrderedService;
import com.candronex.ordermanagement.domain.ServiceCharacteristic;
import com.candronex.ordermanagement.domain.UnknownServiceSpecificationException;
import com.candronex.ordermanagement.domain.port.OrderRepository;
import com.candronex.servicecatalog.published.ServiceCatalog;
import com.candronex.servicecatalog.published.ServiceSpecificationSummary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Optional;

/**
 * Le travail transactionnel d'UC-04 : un cas d'utilisation, une transaction, ouverte et validée par
 * la couche applicative.
 */
@Service
public class OrderPlacement {

    private final OrderRepository orders;
    private final DroneDirectory drones;
    private final ClientDirectory clients;
    private final ServiceCatalog catalog;
    private final Clock clock;

    public OrderPlacement(OrderRepository orders,
                          DroneDirectory drones,
                          ClientDirectory clients,
                          ServiceCatalog catalog,
                          Clock clock) {
        this.orders = orders;
        this.drones = drones;
        this.clients = clients;
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

        if (!clients.exists(command.clientId())) {
            throw new UnknownClientException(command.clientId());
        }
        if (command.orderLines() == null || command.orderLines().isEmpty()) {
            throw new IllegalArgumentException("Une commande doit comporter au moins une ligne.");
        }

        Order order = Order.place(command.clientId(), key, clock.instant());

        for (PlaceOrderCommand.OrderLine requested : command.orderLines()) {
            // Existence et appartenance au client : vérifiées par DroneManagement, propriétaire du drone.
            drones.findForClient(requested.droneId(), command.clientId())
                    .orElseThrow(() -> new DroneNotFoundException(requested.droneId()));

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
                specification.version(),
                specification.characteristics().stream()
                        .map(characteristic -> new ServiceCharacteristic(
                                characteristic.name(), characteristic.value()))
                        .toList());
    }
}
