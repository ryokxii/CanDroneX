package com.candronex.slice.activation.infrastructure.network;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Comportement du réseau simulé.
 *
 * @param latency     latence moyenne d'un appel (± 50 %)
 * @param failureRate part des activations refusées par le réseau
 * @param timeoutRate part des appels qui ne répondent pas dans le délai
 * @param timeout     délai au-delà duquel l'issue est inconnue
 */
@ConfigurationProperties("candronex.network.mock")
public record MockNetworkProperties(Duration latency, double failureRate, double timeoutRate,
                                    Duration timeout) {
}
