package com.example.biyeshiji.repository;

import com.example.biyeshiji.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {
    List<Report> findByStatus(Integer status);
    List<Report> findByTargetTypeAndTargetId(String targetType, Long targetId);
    List<Report> findByReporterId(Long reporterId);
    
    // 根据类型和状态筛选（不区分大小写）
    @Query("SELECT r FROM Report r WHERE LOWER(r.targetType) = LOWER(:targetType) AND r.status = :status")
    List<Report> findByTargetTypeIgnoreCaseAndStatus(@Param("targetType") String targetType, @Param("status") Integer status);
    
    // 根据类型筛选（不区分大小写）
    @Query("SELECT r FROM Report r WHERE LOWER(r.targetType) = LOWER(:targetType)")
    List<Report> findByTargetTypeIgnoreCase(@Param("targetType") String targetType);
}