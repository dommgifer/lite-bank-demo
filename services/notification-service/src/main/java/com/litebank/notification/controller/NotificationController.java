package com.litebank.notification.controller;

import com.litebank.notification.dto.NotificationEvent;
import com.litebank.notification.service.NotificationService;
import com.litebank.notification.service.SseConnectionManager;
import com.litebank.notification.util.JwtUtil;
import io.opentelemetry.api.trace.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final SseConnectionManager connectionManager;
    private final NotificationService notificationService;
    private final Tracer tracer;
    private final JwtUtil jwtUtil;

    /**
     * SSE 串流端點 - 前端透過此端點訂閱通知
     * 支援 Last-Event-ID 斷線重連
     */
    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> stream(
            @RequestHeader(value = "X-User-ID", required = false) String headerUserId,
            @RequestHeader(value = "Last-Event-ID", required = false) String lastEventId,
            @RequestParam(value = "token", required = false) String token) {

        String userId = resolveUserId(headerUserId, token);
        log.info("SSE stream requested by user: {}, lastEventId: {}", userId, lastEventId);

        SseEmitter emitter = connectionManager.register(userId);

        // 如果有 Last-Event-ID，補發漏掉的通知
        if (lastEventId != null && !lastEventId.isEmpty()) {
            try {
                Long lastId = Long.parseLong(lastEventId);
                List<NotificationEvent> missedNotifications =
                        notificationService.getNotificationsAfter(Long.parseLong(userId), lastId);

                for (NotificationEvent event : missedNotifications) {
                    connectionManager.sendToUser(userId, event);
                }
                log.info("Sent {} missed notifications to user {} after reconnect",
                        missedNotifications.size(), userId);
            } catch (NumberFormatException e) {
                log.warn("Invalid Last-Event-ID: {}", lastEventId);
            }
        }

        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.set("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate");
        headers.set("Pragma", "no-cache");
        headers.set("Expires", "0");
        headers.set("Connection", "keep-alive");
        headers.set("X-Accel-Buffering", "no");

        return ResponseEntity.ok()
                .headers(headers)
                .body(emitter);
    }

    /**
     * 取得通知列表（分頁）
     */
    @GetMapping
    public ResponseEntity<Page<NotificationEvent>> getNotifications(
            @RequestHeader(value = "X-User-ID", required = false) String headerUserId,
            @RequestParam(value = "token", required = false) String token,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        String userId = resolveUserId(headerUserId, token);
        Page<NotificationEvent> notifications =
                notificationService.getNotifications(Long.parseLong(userId), page, size);

        return ResponseEntity.ok(notifications);
    }

    /**
     * 取得未讀通知
     */
    @GetMapping("/unread")
    public ResponseEntity<List<NotificationEvent>> getUnreadNotifications(
            @RequestHeader(value = "X-User-ID", required = false) String headerUserId,
            @RequestParam(value = "token", required = false) String token) {

        String userId = resolveUserId(headerUserId, token);
        List<NotificationEvent> notifications =
                notificationService.getUnreadNotifications(Long.parseLong(userId));

        return ResponseEntity.ok(notifications);
    }

    /**
     * 取得未讀通知數量
     */
    @GetMapping("/unread/count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(
            @RequestHeader(value = "X-User-ID", required = false) String headerUserId,
            @RequestParam(value = "token", required = false) String token) {

        String userId = resolveUserId(headerUserId, token);
        long count = notificationService.getUnreadCount(Long.parseLong(userId));

        return ResponseEntity.ok(Map.of("count", count));
    }

    /**
     * 標記單個通知為已讀
     */
    @PutMapping("/{id}/read")
    public ResponseEntity<NotificationEvent> markAsRead(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-ID", required = false) String headerUserId,
            @RequestParam(value = "token", required = false) String token) {

        String userId = resolveUserId(headerUserId, token);

        return notificationService.markAsRead(id, Long.parseLong(userId))
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Notification not found or not owned by user"));
    }

    /**
     * 標記所有通知為已讀
     */
    @PutMapping("/read-all")
    public ResponseEntity<Map<String, Integer>> markAllAsRead(
            @RequestHeader(value = "X-User-ID", required = false) String headerUserId,
            @RequestParam(value = "token", required = false) String token) {

        String userId = resolveUserId(headerUserId, token);
        int count = notificationService.markAllAsRead(Long.parseLong(userId));

        return ResponseEntity.ok(Map.of("markedCount", count));
    }

    /**
     * 刪除所有通知
     */
    @DeleteMapping
    public ResponseEntity<Map<String, Integer>> clearAllNotifications(
            @RequestHeader(value = "X-User-ID", required = false) String headerUserId,
            @RequestParam(value = "token", required = false) String token) {

        String userId = resolveUserId(headerUserId, token);
        int count = notificationService.clearAllNotifications(Long.parseLong(userId));

        return ResponseEntity.ok(Map.of("deletedCount", count));
    }

    /**
     * 取得連線狀態
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus(
            @RequestHeader(value = "X-User-ID", required = false) String userId) {

        Map<String, Object> status = Map.of(
                "totalConnections", connectionManager.getConnectionCount(),
                "userConnected", userId != null && connectionManager.isConnected(userId)
        );

        return ResponseEntity.ok(status);
    }

    /**
     * 健康檢查端點
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "connections", connectionManager.getConnectionCount()
        ));
    }

    /**
     * 解析 User ID（從 header 或 token）
     */
    private String resolveUserId(String headerUserId, String token) {
        String userId = headerUserId;
        if (userId == null && token != null) {
            if (!jwtUtil.validateToken(token)) {
                log.warn("Invalid token provided");
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token");
            }
            userId = jwtUtil.extractUserId(token);
        }

        if (userId == null) {
            log.warn("No user ID available");
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User ID required");
        }

        return userId;
    }
}
