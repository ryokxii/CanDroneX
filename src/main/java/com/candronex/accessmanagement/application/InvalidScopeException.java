package com.candronex.accessmanagement.application;

import java.util.Set;

/** Une portée demandée n'existe pas ou n'est pas permise. */
public class InvalidScopeException extends RuntimeException {

    public InvalidScopeException(Set<String> rejected) {
        super("Portée non permise : " + String.join(" ", rejected));
    }
}
