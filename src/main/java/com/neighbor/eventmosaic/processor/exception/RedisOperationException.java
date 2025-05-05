package com.neighbor.eventmosaic.processor.exception;

public class RedisOperationException extends EmProcessorException {

    public RedisOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
