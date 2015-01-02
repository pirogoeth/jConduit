package me.maiome.jconduit.conduit;

public class ConduitException extends Exception {

    private static final long serialVersionUID = 0;
    private String errorCode;
    private String errorInfo;

    public ConduitException(String message) {
        super(message);
    }

    public ConduitException(String errorCode, String errorInfo) {
        this(String.format("ConduitException [%s]: %s", errorCode, errorInfo));
        this.errorCode = errorCode;
        this.errorInfo = errorInfo;
    }

    public String getErrorCode() {
        return this.errorCode;
    }

    public String getErrorInfo() {
        return this.errorInfo;
    }
}