package com.candronex.order.application;

/** Un service appelé ne répond pas dans le délai, ou répond par une erreur. */
public class DependencyUnavailableException extends RuntimeException {

    public DependencyUnavailableException(String detail, Throwable cause) {
        super(detail, cause);
    }
}
