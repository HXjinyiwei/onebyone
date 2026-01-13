package com.example.biyeshiji.repository;

import com.example.biyeshiji.entity.AdminNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminNotificationRepository extends JpaRepository<AdminNotification, Long> {
    List<AdminNotification> findByAdminRead(Integer adminRead);
    List<AdminNotification> findByStatus(Integer status);
    List<AdminNotification> findByType(Integer type);
    long countByAdminRead(Integer adminRead);
    long countByTypeAndStatus(Integer type, Integer status);
    List<AdminNotification> findByTypeOrderByCreateTimeDesc(Integer type);
}