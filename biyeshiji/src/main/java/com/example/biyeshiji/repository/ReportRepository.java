package com.example.biyeshiji.repository;

import com.example.biyeshiji.entity.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {
    List<Report> findByStatus(Integer status);
    List<Report> findByTargetTypeAndTargetId(String targetType, Long targetId);
    List<Report> findByReporterId(Long reporterId);
}