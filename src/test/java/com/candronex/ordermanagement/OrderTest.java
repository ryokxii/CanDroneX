package com.candronex.ordermanagement;

import com.candronex.ordermanagement.domain.DuplicateOrderLineException;
import com.candronex.ordermanagement.domain.IdempotencyKey;
import com.candronex.ordermanagement.domain.Order;
import com.candronex.ordermanagement.domain.OrderLine;
import com.candronex.ordermanagement.domain.OrderLineStatus;
import com.candronex.ordermanagement.domain.OrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static com.candronex.ordermanagement.CatalogFixtures.C2;
import static com.candronex.ordermanagement.CatalogFixtures.IMAGERY;
import static com.candronex.ordermanagement.CatalogFixtures.c2;
import static com.candronex.ordermanagement.CatalogFixtures.imagery;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Règles de l'agrégat Order : invariant des lignes et cycle de vie. */
class OrderTest {

    private static final UUID CLIENT = UUID.randomUUID();
    private static final UUID DRONE = UUID.randomUUID();
    private static final Instant PLACED = Instant.parse("2026-10-04T14:15:40Z");
    private static final Instant LATER = PLACED.plusSeconds(30);

    private static Order anOrder() {
        return Order.place(CLIENT, IdempotencyKey.of("clé-1"), PLACED);
    }

    @Nested
    @DisplayName("lignes")
    class Lines {

        @Test
        @DisplayName("une ligne ajoutée naît à l'état RECEIVED avec le service commandé figé")
        void newLineStartsAsReceivedWithFrozenService() {
            Order order = anOrder();

            OrderLine line = order.addLine(DRONE, c2());

            assertThat(line.status()).isEqualTo(OrderLineStatus.RECEIVED);
            assertThat(line.droneId()).isEqualTo(DRONE);
            assertThat(line.service().serviceSpecificationId()).isEqualTo(C2);
            assertThat(line.service().characteristics())
                    .extracting(characteristic -> characteristic.name() + "=" + characteristic.value())
                    .contains("sst=2", "sd=000001", "dnn=c2", "fiveQi=7", "arp=2");
        }

        @Test
        @DisplayName("refuse deux lignes pour le même couple drone et spécification")
        void rejectsDuplicateLine() {
            Order order = anOrder();
            order.addLine(DRONE, c2());

            assertThatThrownBy(() -> order.addLine(DRONE, c2()))
                    .isInstanceOf(DuplicateOrderLineException.class);

            assertThat(order.orderLines()).hasSize(1);
        }

        @Test
        @DisplayName("accepte deux services différents pour le même drone, dans l'ordre de la demande")
        void acceptsDifferentServicesForSameDrone() {
            Order order = anOrder();

            order.addLine(DRONE, c2());
            order.addLine(DRONE, imagery());

            assertThat(order.orderLines())
                    .extracting(line -> line.service().serviceSpecificationId())
                    .containsExactly(C2, IMAGERY);
        }

        @Test
        @DisplayName("les lignes ne sont pas modifiables de l'extérieur")
        void exposesLinesAsReadOnly() {
            Order order = anOrder();
            order.addLine(DRONE, c2());

            assertThatThrownBy(() -> order.orderLines().clear())
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("cycle de vie")
    class Lifecycle {

        @Test
        @DisplayName("la commande dérive son état de ses lignes, à chaque transition")
        void derivesStatusOnEveryTransition() {
            Order order = anOrder();
            UUID c2 = order.addLine(DRONE, c2()).orderLineId();
            UUID imagery = order.addLine(DRONE, imagery()).orderLineId();
            assertThat(order.status()).isEqualTo(OrderStatus.RECEIVED);

            order.updateLineStatus(c2, OrderLineStatus.IN_PROGRESS, LATER);
            assertThat(order.status()).isEqualTo(OrderStatus.IN_PROGRESS);

            order.updateLineStatus(c2, OrderLineStatus.COMPLETED, LATER);
            order.updateLineStatus(imagery, OrderLineStatus.IN_PROGRESS, LATER);
            order.updateLineStatus(imagery, OrderLineStatus.FAILED, LATER);
            assertThat(order.status()).isEqualTo(OrderStatus.PARTIAL);
        }

        @Test
        @DisplayName("la date d'achèvement n'existe que lorsque plus aucune ligne n'attend de résultat")
        void completedAtFollowsPendingLines() {
            Order order = anOrder();
            UUID c2 = order.addLine(DRONE, c2()).orderLineId();
            UUID imagery = order.addLine(DRONE, imagery()).orderLineId();

            order.updateLineStatus(c2, OrderLineStatus.IN_PROGRESS, LATER);
            order.updateLineStatus(c2, OrderLineStatus.COMPLETED, LATER);
            assertThat(order.completedAt()).isNull();

            order.updateLineStatus(imagery, OrderLineStatus.IN_PROGRESS, LATER);
            order.updateLineStatus(imagery, OrderLineStatus.FAILED, LATER);
            assertThat(order.status()).isEqualTo(OrderStatus.PARTIAL);
            assertThat(order.completedAt()).isEqualTo(LATER);
        }

        @Test
        @DisplayName("une reprise (UC-10) rouvre la commande et efface sa date d'achèvement")
        void resumptionReopensTheOrder() {
            Order order = anOrder();
            UUID c2 = order.addLine(DRONE, c2()).orderLineId();
            order.updateLineStatus(c2, OrderLineStatus.IN_PROGRESS, LATER);
            order.updateLineStatus(c2, OrderLineStatus.FAILED, LATER);
            assertThat(order.status()).isEqualTo(OrderStatus.FAILED);
            assertThat(order.completedAt()).isEqualTo(LATER);

            order.updateLineStatus(c2, OrderLineStatus.IN_PROGRESS, LATER.plusSeconds(60));

            assertThat(order.status()).isEqualTo(OrderStatus.IN_PROGRESS);
            assertThat(order.completedAt()).isNull();
        }

        @Test
        @DisplayName("refuse une transition interdite : une ligne rendue ne revient pas en arrière")
        void rejectsForbiddenTransitions() {
            Order order = anOrder();
            UUID c2 = order.addLine(DRONE, c2()).orderLineId();

            assertThatThrownBy(() -> order.updateLineStatus(c2, OrderLineStatus.COMPLETED, LATER))
                    .isInstanceOf(IllegalStateException.class);

            order.updateLineStatus(c2, OrderLineStatus.IN_PROGRESS, LATER);
            order.updateLineStatus(c2, OrderLineStatus.COMPLETED, LATER);
            assertThatThrownBy(() -> order.updateLineStatus(c2, OrderLineStatus.RECEIVED, LATER))
                    .isInstanceOf(IllegalStateException.class);
            assertThat(order.status()).isEqualTo(OrderStatus.COMPLETED);
        }

        @Test
        @DisplayName("seule une ligne non commencée peut être annulée")
        void onlyUnstartedLinesCanBeCancelled() {
            Order order = anOrder();
            UUID c2 = order.addLine(DRONE, c2()).orderLineId();
            UUID imagery = order.addLine(DRONE, imagery()).orderLineId();

            order.updateLineStatus(c2, OrderLineStatus.CANCELLED, LATER);
            order.updateLineStatus(imagery, OrderLineStatus.IN_PROGRESS, LATER);

            assertThatThrownBy(() -> order.updateLineStatus(imagery, OrderLineStatus.CANCELLED, LATER))
                    .isInstanceOf(IllegalStateException.class);
        }
    }
}
