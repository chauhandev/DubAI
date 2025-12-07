package com.dAdK.dubAI.exceptions;

public class AudioAnalysisException extends RuntimeException {
    public AudioAnalysisException(String message) {
        super(message);
    }

    public AudioAnalysisException(String message, Throwable cause) {
        super(message, cause);
    }
}