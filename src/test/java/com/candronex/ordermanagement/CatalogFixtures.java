package com.candronex.ordermanagement;

import com.candronex.ordermanagement.domain.OrderedService;
import com.candronex.ordermanagement.domain.ServiceCharacteristic;
import com.candronex.servicecatalog.published.ServiceCatalog;
import com.candronex.servicecatalog.published.ServiceSpecificationSummary;
import com.candronex.servicecatalog.published.ServiceSpecificationSummary.Characteristic;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** L'offre de référence du dossier de domaine, pour les tests du module. */
final class CatalogFixtures {

    static final String C2 = "C2_URLLC";
    static final String IMAGERY = "IMAGERY_EMBB";

    private static final ServiceSpecificationSummary C2_SPECIFICATION =
            new ServiceSpecificationSummary(C2, 1, List.of(
                    new Characteristic("sst", "2"),
                    new Characteristic("sd", "000001"),
                    new Characteristic("dnn", "c2"),
                    new Characteristic("fiveQi", "7"),
                    new Characteristic("arp", "2"),
                    new Characteristic("ambrUplink", "20 Mbps"),
                    new Characteristic("ambrDownlink", "20 Mbps")));

    private static final ServiceSpecificationSummary IMAGERY_SPECIFICATION =
            new ServiceSpecificationSummary(IMAGERY, 1, List.of(
                    new Characteristic("sst", "1"),
                    new Characteristic("sd", "000002"),
                    new Characteristic("dnn", "imagery"),
                    new Characteristic("fiveQi", "9"),
                    new Characteristic("arp", "8"),
                    new Characteristic("ambrUplink", "500 Mbps"),
                    new Characteristic("ambrDownlink", "100 Mbps")));

    private CatalogFixtures() {
    }

    /** Un catalogue en mémoire ; chaque appel en crée un nouveau. */
    static MutableCatalog catalog() {
        return new MutableCatalog();
    }

    static OrderedService c2() {
        return frozen(C2_SPECIFICATION);
    }

    static OrderedService imagery() {
        return frozen(IMAGERY_SPECIFICATION);
    }

    private static OrderedService frozen(ServiceSpecificationSummary specification) {
        return new OrderedService(specification.serviceSpecificationId(), specification.version(),
                specification.characteristics().stream()
                        .map(characteristic -> new ServiceCharacteristic(
                                characteristic.name(), characteristic.value()))
                        .toList());
    }

    /** Catalogue dont l'offre peut évoluer pendant un test. */
    static final class MutableCatalog implements ServiceCatalog {

        private final Map<String, ServiceSpecificationSummary> offer = new HashMap<>(Map.of(
                C2, C2_SPECIFICATION,
                IMAGERY, IMAGERY_SPECIFICATION));

        @Override
        public Optional<ServiceSpecificationSummary> findSpecification(String serviceSpecificationId) {
            return Optional.ofNullable(offer.get(serviceSpecificationId));
        }

        /** Publie une nouvelle version de l'offre C2, avec un autre arp. */
        void publishNewC2Version(String arp) {
            List<Characteristic> characteristics = C2_SPECIFICATION.characteristics().stream()
                    .map(characteristic -> characteristic.name().equals("arp")
                            ? new Characteristic("arp", arp)
                            : characteristic)
                    .toList();
            offer.put(C2, new ServiceSpecificationSummary(C2, 2, characteristics));
        }
    }
}
