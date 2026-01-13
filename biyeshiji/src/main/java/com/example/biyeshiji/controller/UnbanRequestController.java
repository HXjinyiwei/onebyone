package com.example.biyeshiji.controller;

import com.example.biyeshiji.common.Response;
import com.example.biyeshiji.entity.AdminNotification;
import com.example.biyeshiji.entity.User;
import com.example.biyeshiji.repository.AdminNotificationRepository;
import com.example.biyeshiji.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/unban-requests")
public class UnbanRequestController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AdminNotificationRepository adminNotificationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/pending-count")
    public Response<Long> getPendingCount() {
        // 实际查询待处理解封请求数量（type=1且status=0）
        long count = adminNotificationRepository.countByTypeAndStatus(1, 0);
        return Response.success("获取成功", count);
    }

    @GetMapping("/list")
    public Response<List<Map<String, Object>>> listUnbanRequests() {
        // 查询所有解封请求（type=1）
        List<AdminNotification> notifications = adminNotificationRepository.findByTypeOrderByCreateTimeDesc(1);
        List<Map<String, Object>> list = new ArrayList<>();
        for (AdminNotification notification : notifications) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", notification.getId());
            map.put("userId", notification.getSenderId());
            map.put("username", getUserName(notification.getSenderId()));
            map.put("content", notification.getContent());
            map.put("status", notification.getStatus());
            map.put("createTime", notification.getCreateTime());
            list.add(map);
        }
        return Response.success("获取成功", list);
    }

    @PostMapping("/process")
    public Response<Void> processUnbanRequest(@RequestParam Long requestId, @RequestParam String action) {
        // 查找解封请求
        AdminNotification notification = adminNotificationRepository.findById(requestId).orElse(null);
        if (notification == null) {
            return Response.error("解封请求不存在");
        }
        if (notification.getType() != 1) {
            return Response.error("该通知不是解封请求");
        }
        if (notification.getStatus() != 0) {
            return Response.error("该请求已处理，不能重复操作");
        }
        // 根据action更新状态
        int newStatus;
        if ("APPROVED".equalsIgnoreCase(action)) {
            newStatus = 1; // 已通过
            // 解封用户
            User user = userRepository.findById(notification.getSenderId()).orElse(null);
            if (user != null) {
                user.setStatus(0); // 解封
                user.setBanUntil(null);
                userRepository.save(user);
            }
        } else if ("REJECTED".equalsIgnoreCase(action)) {
            newStatus = 2; // 已拒绝
        } else {
            return Response.error("无效的操作类型");
        }
        notification.setStatus(newStatus);
        notification.setUpdateTime(LocalDateTime.now());
        adminNotificationRepository.save(notification);
        return Response.success("处理成功", null);
    }

    @PostMapping("/submit")
    public Response<Void> submitUnbanRequest(@RequestParam String username,
                                             @RequestParam String password,
                                             @RequestParam String content) {
        // 验证用户
        User user = userRepository.findByUsername(username);
        if (user == null) {
            return Response.error("用户不存在");
        }
        // 调试日志
        System.out.println("DEBUG: username=" + username + ", stored hash=" + user.getUserPassword());
        System.out.println("DEBUG: password provided=" + password);
        boolean matches = passwordEncoder.matches(password, user.getUserPassword());
        System.out.println("DEBUG: password matches=" + matches);
        // 验证密码（使用BCrypt加密匹配）
        if (!matches) {
            return Response.error("密码错误");
        }
        // 检查用户是否被封禁（使用isBanned方法，该方法会处理过期自动解封）
        if (user.isBanned()) {
            // 如果封禁已过期，isBanned会更新status并返回false，但不会保存到数据库
            // 为了确保状态一致，如果过期自动解封，则保存用户状态
            if (user.getStatus() == 0) {
                userRepository.save(user);
            }
            // 用户仍处于封禁状态，允许提交申诉
        } else {
            // 用户未被封禁
            return Response.error("你未被封禁，无法提交解封申诉");
        }
        // 创建解封请求通知
        AdminNotification notification = new AdminNotification();
        notification.setType(1); // 1=解封请求
        notification.setTitle("解封请求 - " + username);
        notification.setContent(content);
        notification.setSenderId(user.getId());
        notification.setStatus(0); // 未处理
        notification.setAdminRead(0); // 未读
        notification.setCreateTime(LocalDateTime.now());
        notification.setUpdateTime(LocalDateTime.now());
        adminNotificationRepository.save(notification);

        return Response.success("解封请求提交成功，请等待管理员审核", null);
    }

    private String getUserName(Long userId) {
        if (userId == null) return "未知";
        User user = userRepository.findById(userId).orElse(null);
        return user != null ? user.getUsername() : "未知";
    }
}