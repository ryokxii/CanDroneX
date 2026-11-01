package com.candronex.platform.time;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.Duration;

/** L'horloge est injectée plutôt que lue par Instant.now(). */
@Configuration
public class TimeConfiguration {

    @Bean
    public Clock clock() {
        // Précision de timestamptz : une date relue en base est égale à celle renvoyée.
        return Clock.tick(Clock.systemUTC(), Duration.ofNanos(1_000));
    }
}
