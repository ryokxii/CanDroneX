package com.candronex.ordermanagement.application;

import com.candronex.accessmanagement.published.ClientDirectory;
import com.candronex.accessmanagement.published.UnknownClientException;
import com.candronex.droneregistration.published.DroneDirectory;
import com.candronex.ordermanagement.domain.DroneNotFoundException;
import com.candronex.ordermanagement.domain.IdempotencyKey;
import com.candronex.ordermanagement.domain.Order;
import com.candronex.ordermanagement.domain.port.OrderRepository;
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
    private final Clock clock;

    public OrderPlacement(OrderRepository orders,
                          DroneDirectory drones,
                          ClientDirectory clients,
                          Clock clock) {
        this.orders = orders;
        this.drones = drones;
        this.clients = clients;
        this.clock = clock;
    }

    /** Crée la commande, ou retourne celle qui porte déjà cette clé. */
    @Transactional
    public PlaceOrderResult place(PlaceOrderCommand command, IdempotencyKey key) {
        Optional<Order> alreadyPlaced =
                orders.findByClientIdAndIdempotencyKey(command.clientId(), key);
        if (alreadyPlaced.isPresent()) {
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
            // L'appartenance est vérifiée par DroneRegistration, propriétaire de la donnée.
            drones.findForClient(requested.droneId(), command.clientId())
                    .orElseThrow(() -> new DroneNotFoundException(requested.droneId()));

            order.addLine(requested.droneId(), requested.serviceType());
        }

        orders.save(order);
        return PlaceOrderResult.created(OrderView.from(order));
    }
}
