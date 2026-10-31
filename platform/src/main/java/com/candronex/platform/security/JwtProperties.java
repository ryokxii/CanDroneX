package com.candronex.platform.security;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Clés publiques, émetteur et audience attendus du realm candronex. */
@Validated
@ConfigurationProperties("candronex.jwt")
public record JwtProperties(@NotBlank String jwkSetUri, @NotBlank String issuer, @NotBlank String audience) {
}
