package com.example.biyeshiji.service;

import com.example.biyeshiji.entity.Report;
import com.example.biyeshiji.entity.User;

import java.util.List;

public interface ReportService {
    
    /**
     * 创建举报
     */
    Report createReport(Report report);
    
    /**
     * 获取举报列表（管理员）
     */
    List<Report> getReports(String targetType, Integer status);
    
    /**
     * 获取待处理举报数量
     */
    long getPendingCount();
    
    /**
     * 处理举报
     */
    boolean processReport(Long reportId, Long adminId, String action, String result);
    
    /**
     * 根据ID获取举报
     */
    Report getReportById(Long reportId);
    
    /**
     * 获取用户的举报记录
     */
    List<Report> getReportsByReporter(Long reporterId);
    
    /**
     * 获取目标内容的举报记录
     */
    List<Report> getReportsByTarget(String targetType, Long targetId);
    
    /**
     * 忽略举报
     */
    boolean ignoreReport(Long reportId, Long adminId);
    
    /**
     * 删除举报
     */
    boolean deleteReport(Long reportId);
}