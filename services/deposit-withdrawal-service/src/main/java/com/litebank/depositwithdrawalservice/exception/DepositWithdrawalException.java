package com.litebank.depositwithdrawalservice.exception;

import lombok.Getter;

@Getter
public class DepositWithdrawalException extends RuntimeException {

    private final String errorCode;

    public DepositWithdrawalException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public DepositWithdrawalException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}
