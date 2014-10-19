package edu.umass.cs.runner.system.exceptions;


import edu.umass.cs.runner.BackendType;

public class UnknownBackendException extends Exception {
    public UnknownBackendException(BackendType bt) {
        super(String.format("Unknown backend type: %s", bt.toString()));
    }
}
