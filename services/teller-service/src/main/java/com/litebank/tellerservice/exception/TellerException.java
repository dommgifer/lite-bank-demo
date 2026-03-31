package com.litebank.tellerservice.exception;

import lombok.Getter;

@Getter
public class TellerException extends RuntimeException {

    private final String errorCode;

    public TellerException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public TellerException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}
