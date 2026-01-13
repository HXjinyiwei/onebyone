package com.example.biyeshiji.controller;

import com.example.biyeshiji.common.Response;
import com.example.biyeshiji.entity.Report;
import com.example.biyeshiji.entity.User;
import com.example.biyeshiji.entity.Post;
import com.example.biyeshiji.entity.Novel;
import com.example.biyeshiji.entity.Chapter;
import com.example.biyeshiji.entity.Comment;
import com.example.biyeshiji.repository.UserRepository;
import com.example.biyeshiji.repository.PostRepository;
import com.example.biyeshiji.repository.NovelRepository;
import com.example.biyeshiji.repository.ChapterRepository;
import com.example.biyeshiji.repository.CommentRepository;
import com.example.biyeshiji.service.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/reports")
public class ReportController {

    @Autowired
    private ReportService reportService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PostRepository postRepository;
    
    @Autowired
    private NovelRepository novelRepository;
    
    @Autowired
    private ChapterRepository chapterRepository;
    
    @Autowired
    private CommentRepository commentRepository;

    @GetMapping("/pending-count")
    public Response<Long> getPendingCount() {
        long count = reportService.getPendingCount();
        return Response.success("获取成功", count);
    }

    @GetMapping("/list")
    public Response<List<Map<String, Object>>> listReports(@RequestParam(required = false) String type,
                                                           @RequestParam(required = false) Integer status) {
        List<Report> reports = reportService.getReports(type, status);
        List<Map<String, Object>> result = new ArrayList<>();
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        for (Report report : reports) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", report.getId());
            item.put("targetType", report.getTargetType());
            item.put("targetId", report.getTargetId());
            item.put("reason", report.getReason());
            item.put("description", report.getReason());
            item.put("status", report.getStatus());
            item.put("statusText", getStatusText(report.getStatus()));
            item.put("createTime", report.getCreateTime() != null ?
                report.getCreateTime().format(formatter) : "");
            item.put("handleTime", report.getHandleTime() != null ?
                report.getHandleTime().format(formatter) : "");
            item.put("handleResult", report.getHandleResult());
            
            // 获取举报人信息
            if (report.getReporterId() != null) {
                User reporter = userRepository.findById(report.getReporterId()).orElse(null);
                if (reporter != null) {
                    item.put("reporterName", reporter.getNickname() != null ? reporter.getNickname() : reporter.getUsername());
                    item.put("reporterId", reporter.getId());
                }
            }
            
            // 获取处理管理员信息
            if (report.getAdminId() != null) {
                User admin = userRepository.findById(report.getAdminId()).orElse(null);
                if (admin != null) {
                    item.put("adminName", admin.getUsername());
                }
            }
            
            // 获取目标名称
            String targetName = getTargetName(report.getTargetType(), report.getTargetId());
            item.put("targetName", targetName);
            
            result.add(item);
        }
        
        return Response.success("获取成功", result);
    }

    @PostMapping("/process")
    public Response<Void> processReport(@RequestParam Long reportId, 
                                        @RequestParam String action,
                                        @RequestParam(required = false) String result) {
        // 获取当前管理员ID
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth != null ? auth.getName() : null;
        User admin = username != null ? userRepository.findByUsername(username) : null;
        
        if (admin == null) {
            return Response.error("管理员未登录");
        }
        
        boolean success = reportService.processReport(reportId, admin.getId(), action, result);
        if (success) {
            return Response.success("处理成功", null);
        } else {
            return Response.error("处理失败，举报不存在");
        }
    }
    
    @PostMapping("/ignore")
    public Response<Void> ignoreReport(@RequestParam Long reportId) {
        // 获取当前管理员ID
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth != null ? auth.getName() : null;
        User admin = username != null ? userRepository.findByUsername(username) : null;
        
        if (admin == null) {
            return Response.error("管理员未登录");
        }
        
        boolean success = reportService.ignoreReport(reportId, admin.getId());
        if (success) {
            return Response.success("已忽略", null);
        } else {
            return Response.error("操作失败");
        }
    }
    
    @DeleteMapping("/delete")
    public Response<Void> deleteReport(@RequestParam Long reportId) {
        boolean success = reportService.deleteReport(reportId);
        if (success) {
            return Response.success("删除成功", null);
        } else {
            return Response.error("删除失败");
        }
    }
    
    @GetMapping("/detail")
    public Response<Map<String, Object>> getReportDetail(@RequestParam Long reportId) {
        Report report = reportService.getReportById(reportId);
        if (report == null) {
            return Response.error("举报不存在");
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("id", report.getId());
        result.put("targetType", report.getTargetType());
        result.put("targetId", report.getTargetId());
        result.put("reason", report.getReason());
        result.put("description", report.getReason());
        result.put("status", report.getStatus());
        result.put("statusText", getStatusText(report.getStatus()));
        result.put("createTime", report.getCreateTime());
        result.put("handleTime", report.getHandleTime());
        result.put("handleResult", report.getHandleResult());
        result.put("reporterId", report.getReporterId());
        result.put("adminId", report.getAdminId());
        
        return Response.success("获取成功", result);
    }
    
    // 用户举报API
    @PostMapping("/create")
    public Response<Void> createReport(@RequestBody Map<String, Object> reportData) {
        try {
            // 从Map中提取数据，处理可能的类型转换问题
            Integer targetType = null;
            Object targetTypeObj = reportData.get("targetType");
            if (targetTypeObj != null) {
                if (targetTypeObj instanceof Integer) {
                    targetType = (Integer) targetTypeObj;
                } else if (targetTypeObj instanceof String) {
                    try {
                        targetType = Integer.parseInt((String) targetTypeObj);
                    } catch (NumberFormatException e) {
                        return Response.error("targetType参数格式错误");
                    }
                } else if (targetTypeObj instanceof Number) {
                    targetType = ((Number) targetTypeObj).intValue();
                }
            }
            
            Long targetId = null;
            Object targetIdObj = reportData.get("targetId");
            if (targetIdObj != null) {
                if (targetIdObj instanceof Long) {
                    targetId = (Long) targetIdObj;
                } else if (targetIdObj instanceof Integer) {
                    targetId = ((Integer) targetIdObj).longValue();
                } else if (targetIdObj instanceof String) {
                    try {
                        targetId = Long.parseLong((String) targetIdObj);
                    } catch (NumberFormatException e) {
                        return Response.error("targetId参数格式错误");
                    }
                } else if (targetIdObj instanceof Number) {
                    targetId = ((Number) targetIdObj).longValue();
                }
            }
            
            // reportType参数（可选）
            Integer reportType = null;
            Object reportTypeObj = reportData.get("reportType");
            if (reportTypeObj != null) {
                if (reportTypeObj instanceof Integer) {
                    reportType = (Integer) reportTypeObj;
                } else if (reportTypeObj instanceof String) {
                    try {
                        reportType = Integer.parseInt((String) reportTypeObj);
                    } catch (NumberFormatException e) {
                        // 忽略，使用默认值
                    }
                } else if (reportTypeObj instanceof Number) {
                    reportType = ((Number) reportTypeObj).intValue();
                }
            }
            
            String description = (String) reportData.get("description");
            
            // 验证必要参数
            if (targetType == null) {
                return Response.error("targetType参数不能为空");
            }
            if (targetId == null) {
                return Response.error("targetId参数不能为空");
            }
            if (description == null || description.trim().isEmpty()) {
                return Response.error("举报描述不能为空");
            }
            
            // 获取当前用户ID
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth != null ? auth.getName() : null;
            User reporter = username != null ? userRepository.findByUsername(username) : null;
            
            if (reporter == null) {
                return Response.error("用户未登录");
            }
            
            // 创建Report对象
            Report report = new Report();
            report.setReporterId(reporter.getId());
            report.setTargetType(getTargetTypeString(targetType));
            report.setTargetId(targetId);
            report.setReason(description); // 使用description作为reason
            report.setStatus(0); // 待处理
            
            reportService.createReport(report);
            return Response.success("举报成功，管理员会尽快处理", null);
        } catch (Exception e) {
            e.printStackTrace();
            return Response.error("举报失败：" + e.getMessage());
        }
    }
    
    private String getTargetTypeString(Integer targetType) {
        if (targetType == null) return "unknown";
        switch (targetType) {
            case 1: return "post";
            case 2: return "comment";
            case 3: return "novel";
            case 4: return "chapter";
            case 5: return "user";
            default: return "unknown";
        }
    }
    
    private String getStatusText(Integer status) {
        if (status == null) return "未知";
        switch (status) {
            case 0: return "待处理";
            case 1: return "已处理";
            case 2: return "已忽略";
            default: return "未知";
        }
    }
    
    private String getTargetName(String targetType, Long targetId) {
        if (targetType == null || targetId == null) {
            return "未知";
        }
        
        try {
            switch (targetType.toLowerCase()) {
                case "post":
                    Post post = postRepository.findById(targetId).orElse(null);
                    return post != null ? post.getTitle() : "帖子(ID:" + targetId + ")";
                case "novel":
                    Novel novel = novelRepository.findById(targetId).orElse(null);
                    return novel != null ? novel.getTitle() : "小说(ID:" + targetId + ")";
                case "chapter":
                    Chapter chapter = chapterRepository.findById(targetId).orElse(null);
                    return chapter != null ? chapter.getTitle() : "章节(ID:" + targetId + ")";
                case "comment":
                    Comment comment = commentRepository.findById(targetId).orElse(null);
                    return comment != null ? "评论(ID:" + targetId + ")" : "评论(ID:" + targetId + ")";
                case "user":
                    User user = userRepository.findById(targetId).orElse(null);
                    return user != null ? user.getNickname() != null ? user.getNickname() : user.getUsername() : "用户(ID:" + targetId + ")";
                default:
                    return targetType + "(ID:" + targetId + ")";
            }
        } catch (Exception e) {
            e.printStackTrace();
            return targetType + "(ID:" + targetId + ")";
        }
    }
}