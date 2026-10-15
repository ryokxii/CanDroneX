package com.candronex.servicecatalog;

import com.candronex.servicecatalog.domain.CharacteristicSpecification;
import com.candronex.servicecatalog.domain.CharacteristicValueType;
import com.candronex.servicecatalog.domain.ServiceSpecification;
import com.candronex.servicecatalog.domain.ServiceSpecificationId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Règles de l'agrégat ServiceSpecification : ce que l'offre peut contenir. */
class ServiceSpecificationTest {

    private static final CharacteristicSpecification SST =
            new CharacteristicSpecification("sst", CharacteristicValueType.INTEGER, "2");
    private static final CharacteristicSpecification DNN =
            new CharacteristicSpecification("dnn", CharacteristicValueType.STRING, "c2");

    @Test
    @DisplayName("une spécification garde ses caractéristiques dans l'ordre de publication")
    void keepsCharacteristicsInOrder() {
        ServiceSpecification specification = ServiceSpecification.restore(
                ServiceSpecificationId.of("C2_URLLC"), "C&C Connectivity", 1, List.of(SST, DNN));

        assertThat(specification.characteristics()).containsExactly(SST, DNN);
        assertThatThrownBy(() -> specification.characteristics().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("refuse une caractéristique en double, une version nulle ou une liste vide")
    void rejectsInconsistentSpecification() {
        ServiceSpecificationId id = ServiceSpecificationId.of("C2_URLLC");

        assertThatThrownBy(() -> ServiceSpecification.restore(id, "C2", 1, List.of(SST, SST)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("double");
        assertThatThrownBy(() -> ServiceSpecification.restore(id, "C2", 0, List.of(SST)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ServiceSpecification.restore(id, "C2", 1, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("une valeur entière doit en être une")
    void rejectsValueOfWrongType() {
        assertThatThrownBy(() ->
                new CharacteristicSpecification("sst", CharacteristicValueType.INTEGER, "deux"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"c2_urllc", "2C", "C2-URLLC", "", "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456"})
    @DisplayName("refuse un identifiant hors du format publié")
    void rejectsMalformedIdentifier(String value) {
        assertThat(ServiceSpecificationId.isWellFormed(value)).isFalse();
        assertThatThrownBy(() -> ServiceSpecificationId.of(value))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
