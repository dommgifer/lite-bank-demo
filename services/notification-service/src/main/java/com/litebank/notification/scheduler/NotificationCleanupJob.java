package com.litebank.notification.scheduler;

import com.litebank.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationCleanupJob {

    private final NotificationRepository notificationRepository;

    @Value("${notification.cleanup.read-retention-days:30}")
    private int readRetentionDays;

    @Value("${notification.cleanup.unread-retention-days:90}")
    private int unreadRetentionDays;

    /**
     * 每天凌晨 3 點執行清理任務
     * 清理策略：
     * - 已讀通知：保留 30 天
     * - 未讀通知：保留 90 天（避免無限累積）
     */
    @Scheduled(cron = "${notification.cleanup.cron:0 0 3 * * ?}")
    @Transactional
    public void cleanupOldNotifications() {
        log.info("Starting notification cleanup job...");

        LocalDateTime readCutoff = LocalDateTime.now().minusDays(readRetentionDays);
        LocalDateTime unreadCutoff = LocalDateTime.now().minusDays(unreadRetentionDays);

        // 清理已讀通知
        int readDeleted = notificationRepository.deleteReadNotificationsOlderThan(readCutoff);
        log.info("Deleted {} read notifications older than {} days", readDeleted, readRetentionDays);

        // 清理未讀通知
        int unreadDeleted = notificationRepository.deleteUnreadNotificationsOlderThan(unreadCutoff);
        log.info("Deleted {} unread notifications older than {} days", unreadDeleted, unreadRetentionDays);

        log.info("Notification cleanup job completed. Total deleted: {}", readDeleted + unreadDeleted);
    }
}
