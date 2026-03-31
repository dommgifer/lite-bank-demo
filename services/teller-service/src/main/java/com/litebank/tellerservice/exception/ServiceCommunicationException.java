package com.litebank.tellerservice.exception;

public class ServiceCommunicationException extends RuntimeException {
    public ServiceCommunicationException(String message) {
        super(message);
    }
}
