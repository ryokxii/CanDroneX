package com.candronex.ordermanagement;

import com.candronex.ordermanagement.domain.DuplicateOrderLineException;
import com.candronex.ordermanagement.domain.IdempotencyKey;
import com.candronex.ordermanagement.domain.Order;
import com.candronex.ordermanagement.domain.OrderLine;
import com.candronex.ordermanagement.domain.OrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static com.candronex.servicecatalog.published.ServiceType.C2_URLLC;
import static com.candronex.servicecatalog.published.ServiceType.IMAGERY_EMBB;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Règles de l'agrégat Order : l'invariant d'unicité des lignes et la dérivation de l'état global.
 */
class OrderTest {

    private static final String CLIENT = "CLI-INSPECTRA";
    private static final String DRONE = "DRN-0001";
    private static final Instant NOW = Instant.parse("2026-10-04T14:15:40Z");

    private Order anOrder() {
        return Order.place(CLIENT, IdempotencyKey.of("clé-1"), NOW);
    }

    @Test
    @DisplayName("une ligne ajoutée naît à l'état RECEIVED")
    void newLineStartsAsReceived() {
        Order order = anOrder();

        OrderLine line = order.addLine(DRONE, C2_URLLC);

        assertThat(line.status()).isEqualTo(OrderStatus.RECEIVED);
        assertThat(line.droneId()).isEqualTo(DRONE);
        assertThat(order.orderLines()).hasSize(1);
    }

    @Test
    @DisplayName("refuse deux lignes pour le même couple drone et type de service")
    void rejectsDuplicateLine() {
        Order order = anOrder();
        order.addLine(DRONE, C2_URLLC);

        assertThatThrownBy(() -> order.addLine(DRONE, C2_URLLC))
                .isInstanceOf(DuplicateOrderLineException.class);

        assertThat(order.orderLines()).hasSize(1);
    }

    @Test
    @DisplayName("accepte deux services différents pour le même drone")
    void acceptsDifferentServicesForSameDrone() {
        Order order = anOrder();

        order.addLine(DRONE, C2_URLLC);
        order.addLine(DRONE, IMAGERY_EMBB);

        assertThat(order.orderLines()).hasSize(2);
    }

    @Test
    @DisplayName("les lignes ne sont pas modifiables de l'extérieur")
    void exposesLinesAsReadOnly() {
        Order order = anOrder();
        order.addLine(DRONE, C2_URLLC);

        assertThatThrownBy(() -> order.orderLines().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Nested
    @DisplayName("dérivation de l'état global")
    class StatusDerivation {

        @Test
        @DisplayName("toutes les lignes RECEIVED donnent une commande RECEIVED")
        void allReceived() {
            Order order = anOrder();
            order.addLine(DRONE, C2_URLLC);
            order.addLine(DRONE, IMAGERY_EMBB);

            assertThat(order.status()).isEqualTo(OrderStatus.RECEIVED);
        }

        @Test
        @DisplayName("une ligne encore en cours donne une commande IN_PROGRESS")
        void someStillRunning() {
            Order order = anOrder();
            String first = order.addLine(DRONE, C2_URLLC).orderLineId();
            order.addLine(DRONE, IMAGERY_EMBB);

            order.updateLineStatus(first, OrderStatus.COMPLETED, NOW);

            assertThat(order.status()).isEqualTo(OrderStatus.IN_PROGRESS);
        }

        @Test
        @DisplayName("toutes les lignes COMPLETED donnent une commande COMPLETED")
        void allCompleted() {
            Order order = anOrder();
            String first = order.addLine(DRONE, C2_URLLC).orderLineId();
            String second = order.addLine(DRONE, IMAGERY_EMBB).orderLineId();

            order.updateLineStatus(first, OrderStatus.COMPLETED, NOW);
            order.updateLineStatus(second, OrderStatus.COMPLETED, NOW);

            assertThat(order.status()).isEqualTo(OrderStatus.COMPLETED);
            assertThat(order.completedAt()).isEqualTo(NOW);
        }

        @Test
        @DisplayName("un service rendu et un en échec donnent PARTIALLY_COMPLETED")
        void mixedOutcomesArePartial() {
            Order order = anOrder();
            String c2 = order.addLine(DRONE, C2_URLLC).orderLineId();
            String imagery = order.addLine(DRONE, IMAGERY_EMBB).orderLineId();

            order.updateLineStatus(c2, OrderStatus.COMPLETED, NOW);
            order.updateLineStatus(imagery, OrderStatus.FAILED, NOW);

            // Sans cet état, la commande tomberait en FAILED et masquerait
            // la liaison C2 pourtant active et facturable.
            assertThat(order.status()).isEqualTo(OrderStatus.PARTIALLY_COMPLETED);
        }

        @Test
        @DisplayName("aucune ligne rendue donne une commande FAILED")
        void noneCompletedIsFailed() {
            Order order = anOrder();
            String c2 = order.addLine(DRONE, C2_URLLC).orderLineId();
            String imagery = order.addLine(DRONE, IMAGERY_EMBB).orderLineId();

            order.updateLineStatus(c2, OrderStatus.FAILED, NOW);
            order.updateLineStatus(imagery, OrderStatus.CANCELLED, NOW);

            assertThat(order.status()).isEqualTo(OrderStatus.FAILED);
        }

        @Test
        @DisplayName("toutes les lignes annulées donnent une commande CANCELLED")
        void allCancelled() {
            Order order = anOrder();
            String c2 = order.addLine(DRONE, C2_URLLC).orderLineId();
            String imagery = order.addLine(DRONE, IMAGERY_EMBB).orderLineId();

            order.updateLineStatus(c2, OrderStatus.CANCELLED, NOW);
            order.updateLineStatus(imagery, OrderStatus.CANCELLED, NOW);

            assertThat(order.status()).isEqualTo(OrderStatus.CANCELLED);
        }

        @Test
        @DisplayName("une ligne ne peut pas être partiellement rendue")
        void lineCannotBePartiallyCompleted() {
            Order order = anOrder();
            String line = order.addLine(DRONE, C2_URLLC).orderLineId();

            assertThatThrownBy(() ->
                    order.updateLineStatus(line, OrderStatus.PARTIALLY_COMPLETED, NOW))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
