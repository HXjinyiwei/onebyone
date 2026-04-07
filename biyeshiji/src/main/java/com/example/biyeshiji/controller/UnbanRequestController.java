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
    public Response<List<Map<String, Object>>> listUnbanRequests(@RequestParam(required = false) String status) {
        // 查询解封请求（type=1），支持状态筛选
        List<AdminNotification> notifications;
        
        if (status != null && !status.isEmpty()) {
            if ("processed".equalsIgnoreCase(status)) {
                // 已处理：状态为1（已通过）或2（已拒绝）
                notifications = adminNotificationRepository.findByTypeAndStatusInOrderByCreateTimeDesc(1, List.of(1, 2));
            } else {
                try {
                    int statusInt = Integer.parseInt(status);
                    notifications = adminNotificationRepository.findByTypeAndStatusOrderByCreateTimeDesc(1, statusInt);
                } catch (NumberFormatException e) {
                    // 如果status不是数字，返回空列表
                    notifications = new ArrayList<>();
                }
            }
        } else {
            notifications = adminNotificationRepository.findByTypeOrderByCreateTimeDesc(1);
        }
        
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
        
        Long userId = notification.getSenderId();
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return Response.error("用户不存在");
        }
        
        // 智能处理逻辑：检查用户是否已经被解封（通过其他请求）
        boolean userAlreadyUnbanned = false;
        if ("REJECTED".equalsIgnoreCase(action)) {
            // 检查该用户是否有已通过的解封请求
            List<AdminNotification> approvedRequests = adminNotificationRepository.findBySenderIdAndTypeAndStatus(userId, 1, 1);
            if (!approvedRequests.isEmpty()) {
                userAlreadyUnbanned = true;
            }
        }
        
        // 根据action更新状态
        int newStatus;
        if ("APPROVED".equalsIgnoreCase(action)) {
            newStatus = 1; // 已通过
            // 解封用户
            user.setStatus(0); // 解封
            user.setBanUntil(null);
            userRepository.save(user);
            
            // 将该用户其他待处理的解封请求自动标记为"已处理-重复"
            List<AdminNotification> pendingRequests = adminNotificationRepository.findBySenderIdAndTypeAndStatus(userId, 1, 0);
            for (AdminNotification pending : pendingRequests) {
                if (!pending.getId().equals(requestId)) {
                    pending.setStatus(3); // 3 = 已处理-重复
                    pending.setUpdateTime(LocalDateTime.now());
                    adminNotificationRepository.save(pending);
                }
            }
        } else if ("REJECTED".equalsIgnoreCase(action)) {
            newStatus = 2; // 已拒绝
            
            // 如果用户已经被解封（通过其他请求），则拒绝操作不重新封禁用户
            if (!userAlreadyUnbanned) {
                // 用户没有被解封，可以执行拒绝逻辑
                // 这里可以添加拒绝后的处理，比如保持封禁状态或延长封禁时间
                // 当前逻辑：保持用户当前状态不变
            } else {
                // 用户已经被解封，拒绝操作不影响用户状态
                // 可以添加日志记录
                System.out.println("用户 " + userId + " 已被解封，拒绝操作不影响用户状态");
            }
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
        
        // 频率限制：检查用户最近1小时内是否已提交过解封请求
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        List<AdminNotification> recentRequests = adminNotificationRepository.findBySenderIdAndTypeAndCreateTimeAfter(
            user.getId(), 1, oneHourAgo);
        if (!recentRequests.isEmpty()) {
            // 找到最近1小时内的请求，检查是否有待处理的
            for (AdminNotification req : recentRequests) {
                if (req.getStatus() == 0) {
                    // 有未处理的请求
                    return Response.error("您最近1小时内已提交过解封请求，请等待管理员处理");
                }
            }
            // 虽然有请求，但都已处理，可以继续提交
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
    
    @GetMapping("/list/page")
    public Response<com.example.biyeshiji.common.PaginationResponse<Map<String, Object>>> listUnbanRequestsWithPagination(
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        
        // 查询解封请求（type=1），支持状态筛选
        List<AdminNotification> notifications;
        
        if (status != null && !status.isEmpty()) {
            if ("processed".equalsIgnoreCase(status)) {
                // 已处理：状态为1（已通过）或2（已拒绝）
                notifications = adminNotificationRepository.findByTypeAndStatusInOrderByCreateTimeDesc(1, List.of(1, 2));
            } else {
                try {
                    int statusInt = Integer.parseInt(status);
                    notifications = adminNotificationRepository.findByTypeAndStatusOrderByCreateTimeDesc(1, statusInt);
                } catch (NumberFormatException e) {
                    // 如果status不是数字，返回空列表
                    notifications = new ArrayList<>();
                }
            }
        } else {
            notifications = adminNotificationRepository.findByTypeOrderByCreateTimeDesc(1);
        }
        
        // 计算分页
        long totalRecords = notifications.size();
        if (page == null || page < 1) {
            page = 1;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 10;
        }
        
        int startIndex = (page - 1) * pageSize;
        int endIndex = Math.min(startIndex + pageSize, notifications.size());
        
        // 如果起始索引超出范围，返回空列表
        if (startIndex >= notifications.size()) {
            return Response.success("获取成功", 
                com.example.biyeshiji.common.PaginationResponse.of(
                    new ArrayList<>(), page, pageSize, totalRecords
                )
            );
        }
        
        // 获取当前页数据
        List<AdminNotification> pageNotifications = notifications.subList(startIndex, endIndex);
        List<Map<String, Object>> list = new ArrayList<>();
        for (AdminNotification notification : pageNotifications) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", notification.getId());
            map.put("userId", notification.getSenderId());
            map.put("username", getUserName(notification.getSenderId()));
            map.put("content", notification.getContent());
            map.put("status", notification.getStatus());
            map.put("createTime", notification.getCreateTime());
            list.add(map);
        }
        
        return Response.success("获取成功", 
            com.example.biyeshiji.common.PaginationResponse.of(
                list, page, pageSize, totalRecords
            )
        );
    }
}