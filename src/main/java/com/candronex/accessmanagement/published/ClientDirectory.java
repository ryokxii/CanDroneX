package com.candronex.accessmanagement.published;

import java.util.UUID;

/** Interface publiée par AccessManagement. */
public interface ClientDirectory {

    /**
     * @return vrai si ce client est connu de la plateforme
     */
    boolean exists(UUID clientId);
}
