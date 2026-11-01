package com.candronex.slice.activation.application;

import java.util.UUID;

/** Un service attend un appel au réseau ; publié dans la transaction, traité après sa validation. */
public record ServiceAccepted(UUID serviceId) {
}
