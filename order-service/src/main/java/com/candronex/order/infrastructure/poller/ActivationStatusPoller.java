package com.candronex.order.infrastructure.poller;

import com.candronex.order.application.SyncActivationStatusUseCase;
import com.candronex.platform.logging.CorrelationIdFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Adaptateur entrant : tâche planifiée qui rapproche les lignes en attente de l'état de leur
 * service. Aucun événement ni rappel de Slice Service vers Order Service.
 */
@Component
class ActivationStatusPoller {

    private static final Logger log = LoggerFactory.getLogger(ActivationStatusPoller.class);

    private final SyncActivationStatusUseCase sync;

    ActivationStatusPoller(SyncActivationStatusUseCase sync) {
        this.sync = sync;
    }

    @Scheduled(fixedDelayString = "${candronex.activation.poll-interval}")
    void poll() {
        for (UUID orderId : sync.pendingOrders()) {
            MDC.put(CorrelationIdFilter.MDC_KEY, UUID.randomUUID().toString());
            try {
                sync.sync(orderId);
            } catch (RuntimeException failure) {
                log.error("event=order-sync-error orderId={}", orderId, failure);
            } finally {
                MDC.remove(CorrelationIdFilter.MDC_KEY);
            }
        }
    }
}
