package com.candronex.ordermanagement;

import com.candronex.ordermanagement.domain.OrderLineStatus;
import com.candronex.ordermanagement.domain.OrderStatus;
import com.candronex.ordermanagement.domain.OrderStatusPolicy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Service de domaine : l'état global d'une commande se dérive de l'état de ses lignes. */
class OrderStatusPolicyTest {

    private final OrderStatusPolicy policy = new OrderStatusPolicy();

    private static List<OrderLineStatus> lines(String statuses) {
        return Arrays.stream(statuses.split(" ")).map(OrderLineStatus::valueOf).toList();
    }

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({
            "RECEIVED RECEIVED,         RECEIVED",
            "RECEIVED IN_PROGRESS,      IN_PROGRESS",
            "COMPLETED IN_PROGRESS,     IN_PROGRESS",
            "COMPLETED RECEIVED,        IN_PROGRESS",
            "COMPLETED COMPLETED,       COMPLETED",
            "CANCELLED CANCELLED,       CANCELLED",
            "COMPLETED FAILED,          PARTIAL",
            "COMPLETED CANCELLED,       PARTIAL",
            "FAILED FAILED,             FAILED",
            "FAILED CANCELLED,          FAILED"
    })
    @DisplayName("dérive l'état de la commande")
    void derivesOrderStatus(String lineStatuses, OrderStatus expected) {
        assertThat(policy.derive(lines(lineStatuses))).isEqualTo(expected);
    }

    @Test
    @DisplayName("C2 rendu et imagerie en échec : PARTIAL, jamais FAILED — la liaison C2 reste visible")
    void referenceScenarioIsPartial() {
        assertThat(policy.derive(List.of(OrderLineStatus.COMPLETED, OrderLineStatus.FAILED)))
                .isEqualTo(OrderStatus.PARTIAL);
    }

    @Test
    @DisplayName("une commande sans ligne est RECEIVED")
    void emptyOrderIsReceived() {
        assertThat(policy.derive(List.of())).isEqualTo(OrderStatus.RECEIVED);
    }
}
