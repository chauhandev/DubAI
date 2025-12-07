package com.dAdK.dubAI.exceptions;

public class TtsProcessingException extends RuntimeException {
    public TtsProcessingException(String message) {
        super(message);
    }

    public TtsProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
