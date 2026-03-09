package com.example.biyeshiji.controller;

import com.example.biyeshiji.common.Response;
import com.example.biyeshiji.entity.Comment;
import com.example.biyeshiji.entity.Message;
import com.example.biyeshiji.entity.Post;
import com.example.biyeshiji.entity.User;
import com.example.biyeshiji.repository.UserRepository;
import com.example.biyeshiji.service.CommentService;
import com.example.biyeshiji.service.MessageService;
import com.example.biyeshiji.service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/comment")
public class CommentController {

    @Autowired
    private CommentService commentService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PostService postService;

    @Autowired
    private MessageService messageService;

    @PostMapping("/create")
    public Response<Comment> createComment(@RequestBody Comment comment) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            return Response.error("未登录");
        }
        // 通过用户名查找用户ID
        User currentUser = userRepository.findByUsername(auth.getName());
        if (currentUser == null) {
            return Response.error("用户不存在");
        }
        
        // 检查帖子状态，未审核或已拒绝的帖子不允许评论
        if (comment.getPostId() != null) {
            Post post = postService.getPostById(comment.getPostId());
            if (post == null || post.getIsDeleted() == 1) {
                return Response.error("帖子不存在");
            }
            // 帖子状态：0=正常，1=审核中，2=已拒绝
            if (post.getStatus() != 0) {
                return Response.error("未审核或已拒绝的帖子不允许评论");
            }
        }
        
        comment.setAuthorId(currentUser.getId());
        Comment createdComment = commentService.createComment(comment);
        if (createdComment != null) {
            return Response.success("评论创建成功", createdComment);
        } else {
            return Response.error("评论创建失败");
        }
    }

    @PostMapping("/update")
    public Response<Comment> updateComment(@RequestBody Comment comment) {
        Comment updatedComment = commentService.updateComment(comment);
        if (updatedComment != null) {
            return Response.success("评论更新成功", updatedComment);
        } else {
            return Response.error("评论更新失败");
        }
    }

    @PostMapping("/delete/{id}")
    public Response<Void> deleteComment(@PathVariable Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            return Response.error("未登录");
        }
        User currentUser = userRepository.findByUsername(auth.getName());
        if (currentUser == null) {
            return Response.error("用户不存在");
        }
        
        // 获取评论
        Comment comment = commentService.getCommentById(id);
        if (comment == null || comment.getIsDeleted() == 1) {
            return Response.error("评论不存在");
        }
        
        // 权限检查：管理员、帖子作者、评论作者
        boolean canDelete = false;
        if (currentUser.getRole() == 1) {
            canDelete = true; // 管理员
        } else if (comment.getAuthorId().equals(currentUser.getId())) {
            canDelete = true; // 评论作者本人
        } else {
            // 检查是否为帖子作者
            Post post = postService.getPostById(comment.getPostId());
            if (post != null && post.getAuthorId().equals(currentUser.getId())) {
                canDelete = true; // 帖子作者
            }
        }
        
        if (!canDelete) {
            return Response.error("无权删除此评论");
        }
        
        boolean result = commentService.deleteComment(id);
        if (result) {
            // 发送消息通知给评论作者
            Long authorId = comment.getAuthorId();
            String content = "您的评论（内容：" + comment.getContent().substring(0, Math.min(comment.getContent().length(), 50)) + "）已被删除。";
            Message message = new Message();
            message.setUserId(authorId);
            message.setContent(content);
            message.setCreateTime(LocalDateTime.now());
            message.setIsRead(0);
            messageService.createMessage(message);
            
            return Response.success("评论删除成功", null);
        } else {
            return Response.error("评论删除失败");
        }
    }

    @GetMapping("/detail/{id}")
    public Response<Comment> getCommentById(@PathVariable Long id) {
        Comment comment = commentService.getCommentById(id);
        if (comment != null) {
            return Response.success("获取评论成功", comment);
        } else {
            return Response.error("评论不存在");
        }
    }

    @GetMapping("/post/{postId}")
    public Response<List<Comment>> getCommentsByPostId(@PathVariable Long postId) {
        List<Comment> comments = commentService.getCommentsByPostId(postId);
        return Response.success("获取帖子评论成功", comments);
    }

    @GetMapping("/author/{authorId}")
    public Response<List<Comment>> getCommentsByAuthorId(@PathVariable Long authorId) {
        List<Comment> comments = commentService.getCommentsByAuthorId(authorId);
        return Response.success("获取作者评论成功", comments);
    }
}