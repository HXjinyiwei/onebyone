package com.example.biyeshiji.controller;

import com.example.biyeshiji.common.Response;
import com.example.biyeshiji.entity.Message;
import com.example.biyeshiji.entity.Post;
import com.example.biyeshiji.entity.User;
import com.example.biyeshiji.repository.UserRepository;
import com.example.biyeshiji.service.MessageService;
import com.example.biyeshiji.service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/post")
public class PostController {

    @Autowired
    private PostService postService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private MessageService messageService;


    @PostMapping("/create")
    public Response<Post> createPost(@RequestBody Post post) {
        try {
            Post createdPost = postService.createPost(post);
            if (createdPost != null) {
                return Response.success("帖子创建成功", createdPost);
            } else {
                return Response.error("帖子创建失败");
            }
        } catch (RuntimeException e) {
            return Response.error(e.getMessage());
        }
    }

    @PostMapping("/update")
    public Response<Post> updatePost(@RequestBody Post post) {
        try {
            Post updatedPost = postService.updatePost(post);
            if (updatedPost != null) {
                return Response.success("帖子更新成功", updatedPost);
            } else {
                return Response.error("帖子更新失败");
            }
        } catch (RuntimeException e) {
            return Response.error(e.getMessage());
        }
    }

    @PostMapping("/delete/{id}")
    public Response<Void> deletePost(@PathVariable Long id) {
        boolean result = postService.deletePost(id);
        if (result) {
            return Response.success("帖子删除成功", null);
        } else {
            return Response.error("帖子删除失败");
        }
    }

    @GetMapping("/detail/{id}")
    public Response<Post> getPostById(@PathVariable Long id) {
        Post post = postService.getPostById(id);
        if (post == null || post.getIsDeleted() == 1) {
            return Response.error("帖子不存在");
        }
        
        // 权限检查：未审核帖子只能被作者和管理员看到
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (post.getStatus() != 0) {
            User currentUser = userRepository.findByUsername(auth.getName());
            if (currentUser == null || (!currentUser.getId().equals(post.getAuthorId()) && currentUser.getRole() != 1)) {
                return Response.error("您无权查看此帖子");
            }
        }
        
        // 增加浏览量
        postService.increaseViewCount(id);
        return Response.success("获取帖子成功", post);
    }

    @GetMapping("/all")
    public Response<List<Post>> getAllPosts(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String search) {
        List<Post> posts;
        if (search != null && !search.trim().isEmpty()) {
            // 使用新的搜索功能，同时支持状态筛选
            posts = postService.searchPostsWithStatus(search.trim(), status);
        } else {
            // 使用原有过滤功能
            posts = postService.getPostsWithFilter(categoryId, status, page, pageSize);
        }
        return Response.success("获取帖子列表成功", posts);
    }

    @GetMapping("/category/{categoryId}")
    public Response<List<Post>> getPostsByCategoryId(@PathVariable Long categoryId) {
        List<Post> posts = postService.getPostsByCategoryId(categoryId);
        return Response.success("获取分类帖子成功", posts);
    }

    @GetMapping("/author/{authorId}")
    public Response<List<Post>> getPostsByAuthorId(@PathVariable Long authorId) {
        List<Post> posts = postService.getPostsByAuthorId(authorId);
        return Response.success("获取作者帖子成功", posts);
    }

    @PostMapping("/lock/{id}")
    public Response<Void> lockPost(@PathVariable Long id) {
        boolean result = postService.lockPost(id);
        if (result) {
            return Response.success("帖子锁定成功", null);
        } else {
            return Response.error("帖子锁定失败");
        }
    }

    @PostMapping("/unlock/{id}")
    public Response<Void> unlockPost(@PathVariable Long id) {
        boolean result = postService.unlockPost(id);
        if (result) {
            return Response.success("帖子解锁成功", null);
        } else {
            return Response.error("帖子解锁失败");
        }
    }

    // 审核相关API
    @GetMapping("/pending")
    public Response<List<Post>> getPendingPosts() {
        List<Post> posts = postService.getPostsByStatus(1); // 1表示审核中
        return Response.success("获取待审核帖子成功", posts);
    }

    @PostMapping("/approve/{id}")
    public Response<Void> approvePost(@PathVariable Long id) {
        boolean result = postService.updatePostStatus(id, 0); // 0表示正常
        if (result) {
            return Response.success("帖子审核通过", null);
        } else {
            return Response.error("帖子审核通过失败");
        }
    }

    @PostMapping("/reject/{id}")
    public Response<Void> rejectPost(@PathVariable Long id) {
        boolean result = postService.updatePostStatus(id, 2); // 2表示已拒绝
        if (result) {
            return Response.success("帖子审核拒绝", null);
        } else {
            return Response.error("帖子审核拒绝失败");
        }
    }

    @PostMapping("/reject-with-reason/{id}")
    public Response<Void> rejectPostWithReason(@PathVariable Long id, @RequestBody Map<String, String> requestBody) {
        String reason = requestBody.get("reason");
        boolean result = postService.updatePostStatusWithReason(id, 2, reason); // 2表示已拒绝
        if (result) {
            // 创建消息通知给作者
            Post post = postService.getPostById(id);
            if (post != null) {
                Message message = new Message();
                message.setUserId(post.getAuthorId());
                message.setContent("您的帖子《" + post.getTitle() + "》审核未通过。拒绝原因：" + (reason != null ? reason : "未提供具体原因") + "。请修改后重新提交。");
                messageService.createMessage(message);
            }
            return Response.success("帖子审核拒绝，已通知作者", null);
        } else {
            return Response.error("帖子审核拒绝失败");
        }
    }

    @GetMapping("/liked/{userId}")
    public Response<List<Post>> getPostsLikedByUser(@PathVariable Long userId) {
        List<Post> posts = postService.getPostsLikedByUser(userId);
        return Response.success("获取用户点赞帖子成功", posts);
    }

    @GetMapping("/favorited/{userId}")
    public Response<List<Post>> getPostsFavoritedByUser(@PathVariable Long userId) {
        List<Post> posts = postService.getPostsFavoritedByUser(userId);
        return Response.success("获取用户收藏帖子成功", posts);
    }

    @GetMapping("/search")
    public Response<List<Post>> searchPosts(@RequestParam(required = false) String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            // 如果关键词为空，返回所有帖子（与/all相同但无分页）
            List<Post> posts = postService.getAllPosts();
            return Response.success("搜索成功", posts);
        }
        List<Post> posts = postService.searchPosts(keyword.trim());
        return Response.success("搜索成功", posts);
    }

    @GetMapping("/deleted/{authorId}")
    public Response<List<Post>> getDeletedPostsByAuthor(@PathVariable Long authorId) {
        // 权限检查：只有管理员或用户本人可以查看被删除的帖子
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userRepository.findByUsername(auth.getName());
        if (currentUser == null) {
            return Response.error("未登录");
        }
        if (currentUser.getRole() != 1 && !currentUser.getId().equals(authorId)) {
            return Response.error("无权查看他人的被删除帖子");
        }
        List<Post> deletedPosts = postService.getDeletedPostsByAuthorId(authorId);
        return Response.success("获取被删除帖子成功", deletedPosts);
    }

    @GetMapping("/count")
    public Response<Long> countPosts() {
        long count = postService.countPosts();
        return Response.success("获取帖子总数成功", count);
    }

    // 封禁/解禁API
    @PostMapping("/ban/{id}")
    public Response<Void> banPost(@PathVariable Long id, @RequestParam(required = false) String reason) {
        // 权限检查：管理员（角色1）和高级管理员（角色2）可以封禁
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userRepository.findByUsername(auth.getName());
        if (currentUser == null || (currentUser.getRole() != 1 && currentUser.getRole() != 2)) {
            return Response.error("只有管理员可以封禁帖子");
        }
        
        boolean result = postService.banPost(id, reason);
        if (result) {
            return Response.success("帖子封禁成功", null);
        } else {
            return Response.error("帖子封禁失败");
        }
    }

    @PostMapping("/unban/{id}")
    public Response<Void> unbanPost(@PathVariable Long id) {
        // 权限检查：管理员（角色1）和高级管理员（角色2）可以解禁
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userRepository.findByUsername(auth.getName());
        if (currentUser == null || (currentUser.getRole() != 1 && currentUser.getRole() != 2)) {
            return Response.error("只有管理员可以解禁帖子");
        }
        
        boolean result = postService.unbanPost(id);
        if (result) {
            return Response.success("帖子解禁成功", null);
        } else {
            return Response.error("帖子解禁失败");
        }
    }

    @GetMapping("/banned")
    public Response<List<Post>> getBannedPosts() {
        // 权限检查：只有管理员可以查看封禁帖子列表
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userRepository.findByUsername(auth.getName());
        if (currentUser == null || currentUser.getRole() != 1) {
            return Response.error("只有管理员可以查看封禁帖子列表");
        }
        
        List<Post> posts = postService.getBannedPosts();
        return Response.success("获取封禁帖子列表成功", posts);
    }

    // 重新提交帖子（从拒绝状态改为审核中）
    @PostMapping("/resubmit/{id}")
    public Response<Void> resubmitPost(@PathVariable Long id) {
        try {
            boolean result = postService.resubmitPost(id);
            if (result) {
                return Response.success("帖子已重新提交，等待审核", null);
            } else {
                return Response.error("重新提交失败");
            }
        } catch (RuntimeException e) {
            return Response.error(e.getMessage());
        }
    }
}