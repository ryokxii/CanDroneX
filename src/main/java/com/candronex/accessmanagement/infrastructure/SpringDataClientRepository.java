package com.candronex.accessmanagement.infrastructure;

import com.candronex.accessmanagement.domain.Client;
import org.springframework.data.jpa.repository.JpaRepository;

/** Interface Spring Data, détail d'infrastructure. */
interface SpringDataClientRepository extends JpaRepository<Client, String> {
}
