package com.candronex.dronemanagement.domain;

/** L'ICCID soumis est déjà associé à un autre drone. */
public class DuplicateIccidException extends RuntimeException {

    public DuplicateIccidException() {
        super("Cet ICCID est déjà associé à un drone.");
    }
}
