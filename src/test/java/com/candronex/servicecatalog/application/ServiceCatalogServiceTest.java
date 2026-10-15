package com.candronex.servicecatalog.application;

import com.candronex.servicecatalog.domain.CharacteristicSpecification;
import com.candronex.servicecatalog.domain.CharacteristicValueType;
import com.candronex.servicecatalog.domain.ServiceSpecification;
import com.candronex.servicecatalog.domain.ServiceSpecificationId;
import com.candronex.servicecatalog.domain.port.ServiceSpecificationRepository;
import com.candronex.servicecatalog.published.ServiceSpecificationSummary;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/** L'interface publiée ServiceCatalog, telle que la voit OrderManagement. */
class ServiceCatalogServiceTest {

    private static final ServiceSpecification C2 = ServiceSpecification.restore(
            ServiceSpecificationId.of("C2_URLLC"), "C&C Connectivity", 3, List.of(
                    new CharacteristicSpecification("sst", CharacteristicValueType.INTEGER, "2"),
                    new CharacteristicSpecification("dnn", CharacteristicValueType.STRING, "c2")));

    private final ServiceSpecificationRepository repository =
            id -> id.equals(C2.id()) ? Optional.of(C2) : Optional.empty();

    private final ServiceCatalogService catalog = new ServiceCatalogService(repository);

    @Test
    @DisplayName("publie la version courante et les valeurs de chaque caractéristique")
    void publishesCurrentVersionAndValues() {
        ServiceSpecificationSummary summary = catalog.findSpecification("C2_URLLC").orElseThrow();

        assertThat(summary.serviceSpecificationId()).isEqualTo("C2_URLLC");
        assertThat(summary.version()).isEqualTo(3);
        assertThat(summary.characteristics())
                .extracting(characteristic -> characteristic.name() + "=" + characteristic.value())
                .containsExactly("sst=2", "dnn=c2");
    }

    @Test
    @DisplayName("un identifiant inconnu ou mal formé ne désigne aucun service")
    void unknownOrMalformedIdentifierFindsNothing() {
        assertThat(catalog.findSpecification("VOICE_5G")).isEmpty();
        assertThat(catalog.findSpecification("c2; drop table")).isEmpty();
        assertThat(catalog.findSpecification(null)).isEmpty();
    }
}
