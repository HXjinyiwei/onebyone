package com.example.biyeshiji.repository;

import com.example.biyeshiji.entity.AdminNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AdminNotificationRepository extends JpaRepository<AdminNotification, Long> {
    List<AdminNotification> findByAdminRead(Integer adminRead);
    List<AdminNotification> findByStatus(Integer status);
    List<AdminNotification> findByType(Integer type);
    long countByAdminRead(Integer adminRead);
    long countByTypeAndStatus(Integer type, Integer status);
    List<AdminNotification> findByTypeOrderByCreateTimeDesc(Integer type);
    
    // 根据类型和状态查询
    List<AdminNotification> findByTypeAndStatusOrderByCreateTimeDesc(Integer type, Integer status);
    
    // 根据发送者ID、类型和状态查询
    List<AdminNotification> findBySenderIdAndTypeAndStatus(Long senderId, Integer type, Integer status);
    
    // 根据发送者ID、类型和创建时间之后查询（用于频率限制）
    List<AdminNotification> findBySenderIdAndTypeAndCreateTimeAfter(Long senderId, Integer type, LocalDateTime createTime);
    
    // 根据类型和多个状态查询
    List<AdminNotification> findByTypeAndStatusInOrderByCreateTimeDesc(Integer type, List<Integer> statuses);
}