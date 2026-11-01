package com.candronex.order.domain.port;

import com.candronex.order.domain.OrderLineStatus;

/** État d'un service selon Slice Service, repris tel quel (relation conformiste). */
public enum ServiceState {
    PENDING,
    ACTIVE,
    FAILED,
    UNCONFIRMED;

    /** Un délai dépassé n'est pas un échec : la ligne reste IN_PROGRESS. */
    public OrderLineStatus lineStatus() {
        return switch (this) {
            case PENDING, UNCONFIRMED -> OrderLineStatus.IN_PROGRESS;
            case ACTIVE -> OrderLineStatus.COMPLETED;
            case FAILED -> OrderLineStatus.FAILED;
        };
    }
}
