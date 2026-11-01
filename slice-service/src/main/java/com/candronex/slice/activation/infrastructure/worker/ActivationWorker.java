package com.candronex.slice.activation.infrastructure.worker;

import com.candronex.platform.logging.CorrelationIdFilter;
import com.candronex.slice.activation.application.ActivateServiceUseCase;
import com.candronex.slice.activation.application.ServiceAccepted;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Adaptateur entrant : exécute les appels au réseau hors de la requête HTTP, sur des fils virtuels.
 * Un appel attend surtout le réseau ; la file est le nombre d'appels en cours (loi de Little).
 */
@Component
class ActivationWorker {

    private static final Logger log = LoggerFactory.getLogger(ActivationWorker.class);

    private final ActivateServiceUseCase activation;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private final AtomicInteger inFlight = new AtomicInteger();

    ActivationWorker(ActivateServiceUseCase activation) {
        this.activation = activation;
    }

    /** Après la validation : le service est lisible avant que le réseau ne réponde. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void onAccepted(ServiceAccepted accepted) {
        submit(accepted.serviceId(), activation::attempt);
    }

    /** Services UNCONFIRMED : l'état réel est lu avant toute reprise. */
    @Scheduled(fixedDelayString = "${candronex.activation.recheck-interval}")
    void recheckUnconfirmed() {
        activation.unconfirmed().forEach(serviceId -> submit(serviceId, activation::reconcile));
        if (inFlight.get() > 0) {
            log.info("event=activation-queue inFlight={}", inFlight.get());
        }
    }

    private void submit(UUID serviceId, Consumer<UUID> work) {
        inFlight.incrementAndGet();
        executor.execute(() -> {
            MDC.put(CorrelationIdFilter.MDC_KEY, UUID.randomUUID().toString());
            try {
                work.accept(serviceId);
            } catch (RuntimeException failure) {
                log.error("event=activation-error serviceId={}", serviceId, failure);
            } finally {
                inFlight.decrementAndGet();
                MDC.remove(CorrelationIdFilter.MDC_KEY);
            }
        });
    }

    @PreDestroy
    void stop() {
        executor.close();
    }
}
