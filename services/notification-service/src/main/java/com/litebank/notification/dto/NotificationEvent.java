package com.litebank.notification.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class NotificationEvent {

    private Long id;                    // 資料庫 ID（SSE Last-Event-ID 用）
    private String notificationId;      // 業務 ID（向下相容）
    private String userId;
    private NotificationType type;
    private String title;
    private String message;
    private Map<String, Object> data;
    private Instant timestamp;
    private TraceContext trace;
    private Boolean read;               // 已讀狀態

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TraceContext {
        private String traceId;
        private String spanId;
    }
}
