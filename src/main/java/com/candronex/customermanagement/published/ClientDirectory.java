package com.candronex.customermanagement.published;

import java.util.UUID;

/** Interface publiée par CustomerManagement. */
public interface ClientDirectory {

    /**
     * @return vrai si ce client est connu de la plateforme
     */
    boolean exists(UUID clientId);
}
