package edu.umass.cs.runner.system.exceptions;


import edu.umass.cs.runner.system.backend.KnownBackendType;

public class UnknownBackendException extends Exception {
    public UnknownBackendException(KnownBackendType bt) {
        super(String.format("Unknown backend type: %s", bt.toString()));
    }
}
