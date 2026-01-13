package com.example.biyeshiji.controller;

import com.example.biyeshiji.common.Response;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/notifications")
public class AdminNotificationController {

    @GetMapping("/unread-count")
    public Response<Long> getUnreadCount() {
        // 模拟未读通知数量
        return Response.success("获取成功", 0L);
    }

    @GetMapping("/list")
    public Response<List<Map<String, Object>>> listNotifications() {
        // 模拟通知列表
        List<Map<String, Object>> list = new ArrayList<>();
        return Response.success("获取成功", list);
    }
}