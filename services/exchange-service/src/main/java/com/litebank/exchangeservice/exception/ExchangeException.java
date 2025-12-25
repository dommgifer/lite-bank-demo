package com.litebank.exchangeservice.exception;

public class ExchangeException extends RuntimeException {

    private final String code;

    public ExchangeException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
