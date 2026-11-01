package com.candronex.slice.activation.infrastructure.network;

import com.candronex.slice.activation.domain.port.NetworkProvisioningPort;
import com.candronex.slice.activation.domain.port.ProvisioningRequest;
import com.candronex.slice.activation.domain.port.ProvisioningResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.random.RandomGenerator;

/**
 * Réseau 5G simulé, dans le processus de Slice Service. Un appel trop lent rend TIMEOUT, mais la
 * configuration a pu être appliquée : findStatus le dit, comme le ferait le vrai réseau.
 */
@Component
@ConditionalOnProperty(name = "candronex.network.adapter", havingValue = "mock", matchIfMissing = true)
@EnableConfigurationProperties(MockNetworkProperties.class)
public class MockOpen5GSAdapter implements NetworkProvisioningPort {

    private static final Logger log = LoggerFactory.getLogger(MockOpen5GSAdapter.class);

    /** Attente simulée ; remplaçable dans les tests. */
    interface Sleeper {
        void sleep(Duration duration);
    }

    private final MockNetworkProperties properties;
    private final RandomGenerator random;
    private final Sleeper sleeper;
    private final Set<UUID> configured = ConcurrentHashMap.newKeySet();

    @Autowired
    public MockOpen5GSAdapter(MockNetworkProperties properties) {
        this(properties, new Random(), MockOpen5GSAdapter::pause);
    }

    MockOpen5GSAdapter(MockNetworkProperties properties, RandomGenerator random, Sleeper sleeper) {
        this.properties = properties;
        this.random = random;
        this.sleeper = sleeper;
    }

    @Override
    public ProvisioningResult provision(ProvisioningRequest request) {
        boolean accepted = random.nextDouble() >= properties.failureRate();
        boolean tooSlow = random.nextDouble() < properties.timeoutRate();
        Duration latency = latency();

        if (tooSlow) {
            sleeper.sleep(properties.timeout());
            // Le réseau a pu appliquer la configuration sans répondre à temps.
            if (accepted) {
                configured.add(request.serviceId());
            }
            return ProvisioningResult.TIMEOUT;
        }

        sleeper.sleep(latency);
        if (accepted) {
            configured.add(request.serviceId());
            return ProvisioningResult.SUCCESS;
        }
        return ProvisioningResult.FAILED;
    }

    @Override
    public ProvisioningResult findStatus(UUID serviceId) {
        sleeper.sleep(latency().dividedBy(4));
        ProvisioningResult status = configured.contains(serviceId)
                ? ProvisioningResult.SUCCESS
                : ProvisioningResult.FAILED;
        log.info("event=network-status serviceId={} status={}", serviceId, status);
        return status;
    }

    /** Latence moyenne ± 50 %. */
    private Duration latency() {
        long average = properties.latency().toMillis();
        return Duration.ofMillis(average / 2 + (average == 0 ? 0 : random.nextLong(average + 1)));
    }

    private static void pause(Duration duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }
}
