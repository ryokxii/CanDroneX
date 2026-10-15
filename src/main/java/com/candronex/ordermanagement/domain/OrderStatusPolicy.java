package com.candronex.ordermanagement.domain;

import java.util.Collection;

/**
 * Service de domaine : la politique de calcul de l'état global d'une commande à partir de l'état
 * de ses lignes. Sans état ; la première règle qui s'applique l'emporte.
 *
 * <table>
 *   <caption>Règles, dans l'ordre</caption>
 *   <tr><td>toutes RECEIVED</td><td>RECEIVED</td></tr>
 *   <tr><td>au moins une RECEIVED ou IN_PROGRESS</td><td>IN_PROGRESS</td></tr>
 *   <tr><td>toutes COMPLETED</td><td>COMPLETED</td></tr>
 *   <tr><td>toutes CANCELLED</td><td>CANCELLED</td></tr>
 *   <tr><td>au moins une COMPLETED</td><td>PARTIAL</td></tr>
 *   <tr><td>sinon : FAILED et CANCELLED, aucune COMPLETED</td><td>FAILED</td></tr>
 * </table>
 */
public final class OrderStatusPolicy {

    public OrderStatus derive(Collection<OrderLineStatus> lineStatuses) {
        if (lineStatuses.isEmpty() || allAre(lineStatuses, OrderLineStatus.RECEIVED)) {
            return OrderStatus.RECEIVED;
        }
        if (lineStatuses.stream().anyMatch(OrderLineStatus::isPending)) {
            return OrderStatus.IN_PROGRESS;
        }
        if (allAre(lineStatuses, OrderLineStatus.COMPLETED)) {
            return OrderStatus.COMPLETED;
        }
        if (allAre(lineStatuses, OrderLineStatus.CANCELLED)) {
            return OrderStatus.CANCELLED;
        }
        // Un service rendu n'est jamais masqué par l'échec d'un autre : la liaison C2 active
        // reste visible même si l'imagerie a échoué.
        if (lineStatuses.contains(OrderLineStatus.COMPLETED)) {
            return OrderStatus.PARTIAL;
        }
        return OrderStatus.FAILED;
    }

    private static boolean allAre(Collection<OrderLineStatus> statuses, OrderLineStatus expected) {
        return statuses.stream().allMatch(status -> status == expected);
    }
}
