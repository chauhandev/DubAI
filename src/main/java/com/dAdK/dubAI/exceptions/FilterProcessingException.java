package com.dAdK.dubAI.exceptions;

public class FilterProcessingException extends RuntimeException {

    public FilterProcessingException(String message) {
        super(message);
    }

    public FilterProcessingException(String message, Throwable cause) {
        super(message, cause);
    }

    public FilterProcessingException(Throwable cause) {
        super(cause);
    }
}