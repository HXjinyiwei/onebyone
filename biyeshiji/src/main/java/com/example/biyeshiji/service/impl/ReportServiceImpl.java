package com.example.biyeshiji.service.impl;

import com.example.biyeshiji.entity.Report;
import com.example.biyeshiji.entity.User;
import com.example.biyeshiji.repository.ReportRepository;
import com.example.biyeshiji.repository.UserRepository;
import com.example.biyeshiji.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ReportServiceImpl implements ReportService {

    @Autowired
    private ReportRepository reportRepository;
    
    @Autowired
    private UserRepository userRepository;

    @Override
    @Transactional
    public Report createReport(Report report) {
        // 获取当前登录用户作为举报人
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth != null ? auth.getName() : null;
        User currentUser = username != null ? userRepository.findByUsername(username) : null;
        
        if (currentUser == null) {
            throw new RuntimeException("用户未登录");
        }
        
        // 设置举报人ID
        report.setReporterId(currentUser.getId());
        
        // 设置状态为待处理
        report.setStatus(0);
        
        // 设置创建时间
        report.setCreateTime(LocalDateTime.now());
        
        return reportRepository.save(report);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Report> getReports(String targetType, Integer status) {
        if (targetType != null && !targetType.isEmpty()) {
            if (status != null) {
                // 根据类型和状态筛选（不区分大小写）
                return reportRepository.findByTargetTypeIgnoreCaseAndStatus(targetType, status);
            } else {
                // 只根据类型筛选（不区分大小写）
                return reportRepository.findByTargetTypeIgnoreCase(targetType);
            }
        } else if (status != null) {
            // 只根据状态筛选
            return reportRepository.findByStatus(status);
        } else {
            // 返回所有举报
            return reportRepository.findAll();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public long getPendingCount() {
        return reportRepository.findByStatus(0).size();
    }

    @Override
    @Transactional
    public boolean processReport(Long reportId, Long adminId, String action, String result) {
        Optional<Report> reportOpt = reportRepository.findById(reportId);
        if (reportOpt.isPresent()) {
            Report report = reportOpt.get();
            
            // 更新举报状态
            if ("RESOLVED".equals(action)) {
                report.setStatus(1); // 已处理
            } else if ("IGNORED".equals(action)) {
                report.setStatus(2); // 已忽略
            } else {
                report.setStatus(1); // 默认已处理
            }
            
            report.setAdminId(adminId);
            report.setHandleTime(LocalDateTime.now());
            report.setHandleResult(result);
            
            reportRepository.save(report);
            return true;
        }
        return false;
    }

    @Override
    @Transactional(readOnly = true)
    public Report getReportById(Long reportId) {
        return reportRepository.findById(reportId).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Report> getReportsByReporter(Long reporterId) {
        return reportRepository.findByReporterId(reporterId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Report> getReportsByTarget(String targetType, Long targetId) {
        return reportRepository.findByTargetTypeAndTargetId(targetType, targetId);
    }

    @Override
    @Transactional
    public boolean ignoreReport(Long reportId, Long adminId) {
        return processReport(reportId, adminId, "IGNORED", "管理员忽略此举报");
    }

    @Override
    @Transactional
    public boolean deleteReport(Long reportId) {
        if (reportRepository.existsById(reportId)) {
            reportRepository.deleteById(reportId);
            return true;
        }
        return false;
    }
}