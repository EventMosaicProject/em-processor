package com.neighbor.eventmosaic.processor.exception;

public class RedisSerializationException extends EmProcessorException {

    public RedisSerializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
