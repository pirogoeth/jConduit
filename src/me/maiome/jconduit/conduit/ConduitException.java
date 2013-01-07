package me.maiome.jconduit.conduit;

public class ConduitException extends Exception {

    private static final long serialVersionUID = 0;

    public ConduitException(String message) {
        super(message);
    }

    public ConduitException(String errorCode, String errorInfo) {
        this(String.format("ConduitAPI returned %s: %s", errorCode, errorInfo));
    }
}