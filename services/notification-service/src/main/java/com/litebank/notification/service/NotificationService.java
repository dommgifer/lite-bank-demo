package com.litebank.notification.service;

import com.litebank.notification.dto.NotificationEvent;
import com.litebank.notification.dto.NotificationType;
import com.litebank.notification.entity.Notification;
import com.litebank.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SseConnectionManager sseConnectionManager;

    /**
     * 建立並發送通知（Write-first 設計）
     * 1. 先寫入資料庫（保證持久化）
     * 2. 嘗試 SSE 推送（best effort）
     */
    @Transactional
    public Notification createAndSend(NotificationEvent event) {
        // 1. 寫入資料庫
        Notification notification = Notification.builder()
                .userId(Long.parseLong(event.getUserId()))
                .type(event.getType())
                .title(event.getTitle())
                .message(event.getMessage())
                .metadata(event.getData())
                .build();

        notification = notificationRepository.save(notification);
        log.info("Notification saved to DB: id={}, userId={}, type={}",
                notification.getId(), notification.getUserId(), notification.getType());

        // 2. 嘗試 SSE 推送
        NotificationEvent sseEvent = toEvent(notification);
        boolean sent = sseConnectionManager.sendToUser(event.getUserId(), sseEvent);

        if (sent) {
            log.info("Notification pushed via SSE: id={}", notification.getId());
        } else {
            log.debug("User {} not connected, notification saved for later retrieval", event.getUserId());
        }

        return notification;
    }

    /**
     * 查詢用戶通知（分頁）
     */
    @Transactional(readOnly = true)
    public Page<NotificationEvent> getNotifications(Long userId, int page, int size) {
        Page<Notification> notifications = notificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size));
        return notifications.map(this::toEvent);
    }

    /**
     * 查詢用戶未讀通知
     */
    @Transactional(readOnly = true)
    public List<NotificationEvent> getUnreadNotifications(Long userId) {
        return notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toEvent)
                .toList();
    }

    /**
     * 查詢未讀通知數量
     */
    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    /**
     * SSE 斷線重連：取得 Last-Event-ID 之後的通知
     */
    @Transactional(readOnly = true)
    public List<NotificationEvent> getNotificationsAfter(Long userId, Long lastEventId) {
        return notificationRepository.findByUserIdAndIdGreaterThan(userId, lastEventId)
                .stream()
                .map(this::toEvent)
                .toList();
    }

    /**
     * 標記單個通知為已讀
     */
    @Transactional
    public Optional<NotificationEvent> markAsRead(Long notificationId, Long userId) {
        return notificationRepository.findById(notificationId)
                .filter(n -> n.getUserId().equals(userId))
                .map(notification -> {
                    notification.markAsRead();
                    notificationRepository.save(notification);
                    log.info("Notification marked as read: id={}", notificationId);
                    return toEvent(notification);
                });
    }

    /**
     * 標記用戶所有通知為已讀
     */
    @Transactional
    public int markAllAsRead(Long userId) {
        int count = notificationRepository.markAllAsReadByUserId(userId);
        log.info("Marked {} notifications as read for user {}", count, userId);
        return count;
    }

    /**
     * 刪除用戶所有通知
     */
    @Transactional
    public int clearAllNotifications(Long userId) {
        int count = notificationRepository.deleteAllByUserId(userId);
        log.info("Deleted {} notifications for user {}", count, userId);
        return count;
    }

    /**
     * Entity 轉換為 Event DTO
     */
    private NotificationEvent toEvent(Notification notification) {
        return NotificationEvent.builder()
                .id(notification.getId())
                .notificationId(String.valueOf(notification.getId()))
                .userId(String.valueOf(notification.getUserId()))
                .type(notification.getType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .data(notification.getMetadata())
                .timestamp(notification.getCreatedAt()
                        .atZone(ZoneId.systemDefault())
                        .toInstant())
                .read(notification.getRead())
                .build();
    }
}
