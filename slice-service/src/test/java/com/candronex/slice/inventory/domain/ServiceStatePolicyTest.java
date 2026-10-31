package com.candronex.slice.inventory.domain;

import com.candronex.slice.activation.domain.ServiceState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ServiceStatePolicyTest {

    private final ServiceStatePolicy policy = new ServiceStatePolicy();

    @Test
    @DisplayName("transitions permises du cycle de vie")
    void allowedTransitions() {
        assertThat(policy.allows(ServiceState.PENDING, ServiceState.ACTIVE)).isTrue();
        assertThat(policy.allows(ServiceState.PENDING, ServiceState.UNCONFIRMED)).isTrue();
        assertThat(policy.allows(ServiceState.UNCONFIRMED, ServiceState.ACTIVE)).isTrue();
        assertThat(policy.allows(ServiceState.UNCONFIRMED, ServiceState.PENDING)).isTrue();
        assertThat(policy.allows(ServiceState.FAILED, ServiceState.PENDING)).isTrue();
    }

    @Test
    @DisplayName("un service ACTIVE ne change plus ; UNCONFIRMED n'est pas un échec")
    void forbiddenTransitions() {
        assertThat(policy.allows(ServiceState.ACTIVE, ServiceState.PENDING)).isFalse();
        assertThat(policy.allows(ServiceState.UNCONFIRMED, ServiceState.FAILED)).isFalse();
    }

    @Test
    @DisplayName("l'inventaire refuse une transition interdite et ignore une répétition")
    void inventoryEntryAppliesThePolicy() {
        InventoryService entry = InventoryService.register(
                UUID.randomUUID(), UUID.randomUUID(), ServiceState.ACTIVE, Instant.EPOCH);

        entry.record(ServiceState.ACTIVE, Instant.now());
        assertThat(entry.updatedAt()).isEqualTo(Instant.EPOCH);
        assertThatThrownBy(() -> entry.record(ServiceState.PENDING, Instant.now()))
                .isInstanceOf(IllegalStateException.class);
    }
}
