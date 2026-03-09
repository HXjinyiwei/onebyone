package com.example.biyeshiji.controller;

import com.example.biyeshiji.common.Response;
import com.example.biyeshiji.entity.*;
import com.example.biyeshiji.repository.UserRepository;
import com.example.biyeshiji.service.FavoriteService;
import com.example.biyeshiji.service.PostService;
import com.example.biyeshiji.service.NovelService;
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
    
    @Autowired
    private PostService postService;
    
    @Autowired
    private NovelService novelService;

    /**
     * 检查内容状态，未审核或已拒绝的内容不允许操作
     * @param targetType 目标类型：1=帖子，2=评论，3=小说，4=章节
     * @param targetId 目标ID
     * @return 如果内容状态不允许操作，返回错误响应；否则返回null
     */
    private Response<Void> checkContentStatus(Integer targetType, Long targetId) {
        if (targetType == 1) {
            // 帖子：检查帖子状态
            Post post = postService.getPostById(targetId);
            if (post == null || post.getIsDeleted() == 1) {
                return Response.error("帖子不存在");
            }
            // 帖子状态：0=正常，1=审核中，2=已拒绝
            if (post.getStatus() != 0) {
                return Response.error("未审核或已拒绝的帖子不允许操作");
            }
        } else if (targetType == 3) {
            // 小说：检查小说审核状态
            Novel novel = novelService.getNovelById(targetId);
            if (novel == null || novel.getIsDeleted() == 1) {
                return Response.error("小说不存在");
            }
            // 小说审核状态：0=待审核，1=审核通过，2=审核拒绝，3=封禁
            if (novel.getAuditStatus() != 1) {
                return Response.error("未审核或已拒绝的小说不允许操作");
            }
        }
        // 评论和章节暂时不检查（评论依赖于帖子状态，章节依赖于小说状态）
        return null;
    }

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
        
        // 检查内容状态，未审核或已拒绝的内容不允许收藏
        Response<Void> statusCheck = checkContentStatus(targetType, targetId);
        if (statusCheck != null) {
            return statusCheck;
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
        
        // 如果是收藏操作（不是取消收藏），检查内容状态
        boolean isFavorited = favoriteService.isFavorited(currentUser.getId(), targetType, targetId);
        if (!isFavorited) {
            // 尝试收藏前检查内容状态
            Response<Void> statusCheck = checkContentStatus(targetType, targetId);
            if (statusCheck != null) {
                // 返回错误，但需要保持Response<Boolean>类型
                return Response.error(statusCheck.getMessage());
            }
        }
        
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