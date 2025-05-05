package com.neighbor.eventmosaic.processor.exception;

public class EmProcessorException extends RuntimeException {

    public EmProcessorException(String message) {
        super(message);
    }

    public EmProcessorException(String message, Throwable cause) {
        super(message, cause);
    }
}
