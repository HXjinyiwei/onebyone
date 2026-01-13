package com.example.biyeshiji.controller;

import com.example.biyeshiji.common.Response;
import com.example.biyeshiji.entity.User;
import com.example.biyeshiji.repository.UserRepository;
import com.example.biyeshiji.service.FavoriteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/favorite")
public class FavoriteController {

    @Autowired
    private FavoriteService favoriteService;
    
    @Autowired
    private UserRepository userRepository;

    // 旧端点（仅帖子）保持兼容
    @PostMapping("/add")
    public Response<Void> favoritePost(@RequestParam Long postId) {
        return favorite(1, postId);
    }

    @PostMapping("/remove")
    public Response<Void> unfavoritePost(@RequestParam Long postId) {
        return unfavorite(1, postId);
    }

    @PostMapping("/toggle")
    public Response<Boolean> toggleFavorite(@RequestParam Long postId) {
        return toggle(1, postId);
    }

    @GetMapping("/check")
    public Response<Boolean> checkFavorite(@RequestParam Long postId) {
        return check(1, postId);
    }

    // 新通用端点
    @PostMapping("/addGeneric")
    public Response<Void> favoriteGeneric(@RequestParam Integer targetType, @RequestParam Long targetId) {
        return favorite(targetType, targetId);
    }

    @PostMapping("/removeGeneric")
    public Response<Void> unfavoriteGeneric(@RequestParam Integer targetType, @RequestParam Long targetId) {
        return unfavorite(targetType, targetId);
    }

    @PostMapping("/toggleGeneric")
    public Response<Boolean> toggleGeneric(@RequestParam Integer targetType, @RequestParam Long targetId) {
        return toggle(targetType, targetId);
    }

    @GetMapping("/checkGeneric")
    public Response<Boolean> checkGeneric(@RequestParam Integer targetType, @RequestParam Long targetId) {
        return check(targetType, targetId);
    }

    // 私有辅助方法
    private Response<Void> favorite(Integer targetType, Long targetId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            return Response.error("未登录");
        }
        User currentUser = userRepository.findByUsername(auth.getName());
        if (currentUser == null) {
            return Response.error("用户不存在");
        }
        boolean result = favoriteService.favorite(currentUser.getId(), targetType, targetId);
        if (result) {
            return Response.success("收藏成功", null);
        } else {
            return Response.error("已经收藏过了");
        }
    }

    private Response<Void> unfavorite(Integer targetType, Long targetId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            return Response.error("未登录");
        }
        User currentUser = userRepository.findByUsername(auth.getName());
        if (currentUser == null) {
            return Response.error("用户不存在");
        }
        boolean result = favoriteService.unfavorite(currentUser.getId(), targetType, targetId);
        if (result) {
            return Response.success("取消收藏成功", null);
        } else {
            return Response.error("没有收藏记录");
        }
    }

    private Response<Boolean> toggle(Integer targetType, Long targetId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            return Response.error("未登录");
        }
        User currentUser = userRepository.findByUsername(auth.getName());
        if (currentUser == null) {
            return Response.error("用户不存在");
        }
        boolean isFavorited = favoriteService.isFavorited(currentUser.getId(), targetType, targetId);
        boolean result;
        if (isFavorited) {
            result = favoriteService.unfavorite(currentUser.getId(), targetType, targetId);
        } else {
            result = favoriteService.favorite(currentUser.getId(), targetType, targetId);
        }
        if (result) {
            return Response.success(isFavorited ? "取消收藏成功" : "收藏成功", !isFavorited);
        } else {
            return Response.error(isFavorited ? "取消收藏失败" : "收藏失败");
        }
    }

    private Response<Boolean> check(Integer targetType, Long targetId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            return Response.success("未登录", false);
        }
        User currentUser = userRepository.findByUsername(auth.getName());
        if (currentUser == null) {
            return Response.success("用户不存在", false);
        }
        boolean isFavorited = favoriteService.isFavorited(currentUser.getId(), targetType, targetId);
        return Response.success("检查成功", isFavorited);
    }
}