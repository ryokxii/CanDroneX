package com.candronex.droneregistration.domain;

/** L'IMSI soumis est déjà associé à un autre drone. */
public class DuplicateImsiException extends RuntimeException {

    public DuplicateImsiException() {
        super("Cet IMSI est déjà associé à un drone.");
    }
}
