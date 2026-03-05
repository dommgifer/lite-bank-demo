package com.litebank.notification.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum NotificationType {
    // 轉帳相關
    TRANSFER_SENT,          // 你轉出了錢
    TRANSFER_RECEIVED,      // 你收到了錢
    TRANSFER_FAILED,        // 轉帳失敗

    // 存提款相關
    DEPOSIT_SUCCESS,        // 存款成功
    WITHDRAWAL_SUCCESS,     // 提款成功

    // 換匯相關
    EXCHANGE_SUCCESS,       // 換匯成功

    // 系統通知
    SYSTEM;                 // 系統通知

    /**
     * Jackson 反序列化時使用，支援向下相容舊的 enum 值
     */
    @JsonCreator
    public static NotificationType fromValue(String value) {
        if (value == null) {
            return null;
        }
        return switch (value) {
            // 向下相容舊的 enum 值
            case "TRANSFER_COMPLETED" -> TRANSFER_SENT;
            case "DEPOSIT_COMPLETED" -> DEPOSIT_SUCCESS;
            case "WITHDRAWAL_COMPLETED" -> WITHDRAWAL_SUCCESS;
            case "EXCHANGE_COMPLETED" -> EXCHANGE_SUCCESS;
            case "BALANCE_LOW" -> SYSTEM;
            // 新的 enum 值
            default -> valueOf(value);
        };
    }

    @JsonValue
    public String toValue() {
        return this.name();
    }
}
