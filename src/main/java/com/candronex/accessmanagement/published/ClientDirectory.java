package com.candronex.accessmanagement.published;

/** Interface publiée par AccessManagement. */
public interface ClientDirectory {

    /**
     * @return vrai si ce client est connu de la plateforme
     */
    boolean exists(String clientId);
}
