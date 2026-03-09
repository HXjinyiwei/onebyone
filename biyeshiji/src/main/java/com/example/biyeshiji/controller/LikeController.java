package com.example.biyeshiji.controller;

import com.example.biyeshiji.common.Response;
import com.example.biyeshiji.entity.*;
import com.example.biyeshiji.repository.*;
import com.example.biyeshiji.service.LikeService;
import com.example.biyeshiji.service.PostService;
import com.example.biyeshiji.service.NovelService;
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
        
        // 检查内容状态，未审核或已拒绝的内容不允许点赞
        Response<Void> statusCheck = checkContentStatus(targetType, targetId);
        if (statusCheck != null) {
            return statusCheck;
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
        
        // 如果是点赞操作（不是取消点赞），检查内容状态
        boolean isLiked = likeService.isLiked(currentUser.getId(), targetType, targetId);
        if (!isLiked) {
            // 尝试点赞前检查内容状态
            Response<Void> statusCheck = checkContentStatus(targetType, targetId);
            if (statusCheck != null) {
                // 返回错误，但需要保持Response<Boolean>类型
                return Response.error(statusCheck.getMessage());
            }
        }
        
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