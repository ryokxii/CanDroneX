package com.candronex.ordermanagement.domain;

/** La ligne demande un service qui ne figure pas au catalogue. */
public class UnknownServiceSpecificationException extends RuntimeException {

    public UnknownServiceSpecificationException(String serviceSpecificationId) {
        super("Le service " + serviceSpecificationId + " ne figure pas au catalogue.");
    }
}
