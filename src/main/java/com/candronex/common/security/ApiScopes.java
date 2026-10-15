package com.candronex.common.security;

import java.util.Set;

/** Portées OAuth 2.0 de l'API publique. */
public final class ApiScopes {

    public static final String DRONES_READ = "drones:read";
    public static final String DRONES_WRITE = "drones:write";
    public static final String ORDERS_WRITE = "orders:write";

    /** orders:read sera ajoutée avec le suivi de commande (UC-05). */
    public static final Set<String> ALL = Set.of(DRONES_READ, DRONES_WRITE, ORDERS_WRITE);

    private ApiScopes() {
    }
}
