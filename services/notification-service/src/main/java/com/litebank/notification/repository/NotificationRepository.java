package com.litebank.notification.repository;

import com.litebank.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * 查詢用戶的所有通知（分頁、按時間倒序）
     */
    Page<Notification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * 查詢用戶的未讀通知
     */
    List<Notification> findByUserIdAndReadFalseOrderByCreatedAtDesc(Long userId);

    /**
     * 查詢用戶的未讀通知數量
     */
    long countByUserIdAndReadFalse(Long userId);

    /**
     * SSE 斷線重連：查詢特定 ID 之後的所有通知
     */
    @Query("SELECT n FROM Notification n WHERE n.userId = :userId AND n.id > :lastId ORDER BY n.id ASC")
    List<Notification> findByUserIdAndIdGreaterThan(@Param("userId") Long userId, @Param("lastId") Long lastId);

    /**
     * 批次標記用戶的所有通知為已讀
     */
    @Modifying
    @Query("UPDATE Notification n SET n.read = true, n.readAt = CURRENT_TIMESTAMP WHERE n.userId = :userId AND n.read = false")
    int markAllAsReadByUserId(@Param("userId") Long userId);

    /**
     * 清理已讀的舊通知（超過指定天數）
     */
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.read = true AND n.createdAt < :cutoffDate")
    int deleteReadNotificationsOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * 清理未讀的超舊通知（避免無限累積）
     */
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.read = false AND n.createdAt < :cutoffDate")
    int deleteUnreadNotificationsOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * 刪除用戶的所有通知
     */
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.userId = :userId")
    int deleteAllByUserId(@Param("userId") Long userId);
}
