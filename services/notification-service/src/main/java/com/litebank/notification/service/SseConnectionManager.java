package com.litebank.notification.service;

import com.litebank.notification.dto.NotificationEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class SseConnectionManager {

    // userId -> SseEmitter
    private final Map<String, SseEmitter> connections = new ConcurrentHashMap<>();

    // Scheduler for sending heartbeats to keep connections alive
    private final ScheduledExecutorService heartbeatScheduler = Executors.newSingleThreadScheduledExecutor();

    /**
     * 註冊新的 SSE 連線
     */
    public SseEmitter register(String userId) {
        // 如果已存在連線，先關閉
        SseEmitter existingEmitter = connections.get(userId);
        if (existingEmitter != null) {
            log.info("Closing existing SSE connection for user: {}", userId);
            existingEmitter.complete();
            connections.remove(userId);
        }

        // 建立新的 emitter，timeout 設為 0 表示無限期
        SseEmitter emitter = new SseEmitter(0L);
        connections.put(userId, emitter);

        // 設定回調處理 - 使用 final 引用來避免競態條件
        final SseEmitter thisEmitter = emitter;

        emitter.onCompletion(() -> {
            log.info("SSE connection completed for user: {} (emitter: {})", userId, System.identityHashCode(thisEmitter));
            // 只有當 map 中的 emitter 是當前這個時才移除，避免移除新連線
            boolean removed = connections.remove(userId, thisEmitter);
            log.debug("SSE connection removed: {} for user: {}", removed, userId);
        });

        emitter.onTimeout(() -> {
            log.warn("SSE connection TIMEOUT for user: {} (emitter: {})", userId, System.identityHashCode(thisEmitter));
            connections.remove(userId, thisEmitter);
        });

        emitter.onError(e -> {
            log.error("SSE connection ERROR for user: {} (emitter: {})", userId, System.identityHashCode(thisEmitter), e);
            connections.remove(userId, thisEmitter);
        });

        log.info("SSE connection registered for user: {}, total connections: {}",
                userId, connections.size());

        // 發送初始連線成功事件，然後啟動心跳
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("{\"status\":\"connected\"}"));

            // 立即發送第一個 heartbeat（瀏覽器可能需要更頻繁的數據）
            emitter.send(SseEmitter.event().comment("heartbeat"));

            // 啟動心跳機制來保持連接活躍
            startHeartbeat(userId, thisEmitter);
        } catch (IOException e) {
            log.error("Failed to send initial event to user: {}", userId, e);
            connections.remove(userId, thisEmitter);
        }

        return emitter;
    }

    /**
     * 推送通知給特定用戶
     * 包含 event id 用於 Last-Event-ID 斷線重連
     */
    public boolean sendToUser(String userId, NotificationEvent event) {
        SseEmitter emitter = connections.get(userId);
        if (emitter == null) {
            log.debug("No SSE connection found for user: {}", userId);
            return false;
        }

        try {
            SseEmitter.SseEventBuilder eventBuilder = SseEmitter.event()
                    .name("notification")
                    .data(event);

            // 設定 event id 用於 Last-Event-ID 機制
            if (event.getId() != null) {
                eventBuilder.id(String.valueOf(event.getId()));
            }

            emitter.send(eventBuilder);
            log.info("Notification sent to user: {}, type: {}, id: {}",
                    userId, event.getType(), event.getId());
            return true;
        } catch (IOException e) {
            log.error("Failed to send notification to user: {}", userId, e);
            connections.remove(userId);
            return false;
        }
    }

    /**
     * 廣播通知給所有連線用戶
     */
    public void broadcast(NotificationEvent event) {
        log.info("Broadcasting notification to {} users", connections.size());
        connections.forEach((userId, emitter) -> {
            try {
                emitter.send(SseEmitter.event()
                        .name("notification")
                        .data(event));
            } catch (IOException e) {
                log.error("Failed to broadcast to user: {}", userId, e);
                connections.remove(userId);
            }
        });
    }

    /**
     * 取得目前連線數
     */
    public int getConnectionCount() {
        return connections.size();
    }

    /**
     * 檢查用戶是否已連線
     */
    public boolean isConnected(String userId) {
        return connections.containsKey(userId);
    }

    /**
     * 關閉特定用戶的連線
     */
    public void disconnect(String userId) {
        SseEmitter emitter = connections.remove(userId);
        if (emitter != null) {
            emitter.complete();
            log.info("SSE connection closed for user: {}", userId);
        }
    }

    /**
     * 啟動心跳機制以保持 SSE 連接活躍
     * 每 1 秒發送一個心跳事件（SSE comment）
     * 瀏覽器 EventSource 可能有較短的超時時間，所以需要非常頻繁地發送心跳
     */
    private void startHeartbeat(String userId, SseEmitter emitter) {
        heartbeatScheduler.scheduleAtFixedRate(() -> {
            // 檢查連接是否還在
            if (connections.get(userId) != emitter) {
                log.debug("Heartbeat stopped for user: {} (connection replaced or closed)", userId);
                return;
            }

            try {
                // 發送 SSE comment 作為心跳 (以冒號開頭的行是 SSE 註釋)
                emitter.send(SseEmitter.event().comment("heartbeat"));
                log.trace("Heartbeat sent to user: {}", userId);
            } catch (IOException e) {
                log.debug("Heartbeat failed for user: {}, removing connection", userId);
                connections.remove(userId, emitter);
            }
        }, 1, 1, TimeUnit.SECONDS);
    }
}
