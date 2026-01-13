package com.example.biyeshiji.controller;

import com.example.biyeshiji.common.Response;
import com.example.biyeshiji.entity.User;
import com.example.biyeshiji.repository.UserRepository;
import com.example.biyeshiji.service.LikeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/like")
public class LikeController {

    @Autowired
    private LikeService likeService;
    
    @Autowired
    private UserRepository userRepository;

    @PostMapping("/add")
    public Response<Void> like(@RequestParam Integer targetType, @RequestParam Long targetId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            return Response.error("未登录");
        }
        String username = auth.getName();
        User currentUser = userRepository.findByUsername(username);
        if (currentUser == null) {
            return Response.error("用户不存在");
        }
        boolean result = likeService.like(currentUser.getId(), targetType, targetId);
        if (result) {
            return Response.success("点赞成功", null);
        } else {
            return Response.error("已经点赞过了");
        }
    }

    @PostMapping("/remove")
    public Response<Void> unlike(@RequestParam Integer targetType, @RequestParam Long targetId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            return Response.error("未登录");
        }
        String username = auth.getName();
        User currentUser = userRepository.findByUsername(username);
        if (currentUser == null) {
            return Response.error("用户不存在");
        }
        boolean result = likeService.unlike(currentUser.getId(), targetType, targetId);
        if (result) {
            return Response.success("取消点赞成功", null);
        } else {
            return Response.error("没有点赞记录");
        }
    }

    @PostMapping("/toggle")
    public Response<Boolean> toggleLike(@RequestParam Integer targetType, @RequestParam Long targetId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            return Response.error("未登录");
        }
        String username = auth.getName();
        User currentUser = userRepository.findByUsername(username);
        if (currentUser == null) {
            return Response.error("用户不存在");
        }
        boolean isLiked = likeService.isLiked(currentUser.getId(), targetType, targetId);
        boolean result;
        if (isLiked) {
            result = likeService.unlike(currentUser.getId(), targetType, targetId);
        } else {
            result = likeService.like(currentUser.getId(), targetType, targetId);
        }
        if (result) {
            return Response.success(isLiked ? "取消点赞成功" : "点赞成功", !isLiked);
        } else {
            return Response.error(isLiked ? "取消点赞失败" : "点赞失败");
        }
    }

    @GetMapping("/check")
    public Response<Boolean> checkLike(@RequestParam Integer targetType, @RequestParam Long targetId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            return Response.success("未登录", false);
        }
        String username = auth.getName();
        User currentUser = userRepository.findByUsername(username);
        if (currentUser == null) {
            return Response.success("用户不存在", false);
        }
        boolean isLiked = likeService.isLiked(currentUser.getId(), targetType, targetId);
        return Response.success("检查成功", isLiked);
    }
}