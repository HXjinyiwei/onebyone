package com.example.biyeshiji.service.impl;

import com.example.biyeshiji.entity.Comment;
import com.example.biyeshiji.entity.Message;
import com.example.biyeshiji.entity.Post;
import com.example.biyeshiji.repository.CommentRepository;
import com.example.biyeshiji.service.CommentService;
import com.example.biyeshiji.service.MessageService;
import com.example.biyeshiji.service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class CommentServiceImpl implements CommentService {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private PostService postService;
    
    @Autowired
    private MessageService messageService;

    @Override
    public Comment createComment(Comment comment) {
        // 设置楼层号
        comment.setFloorNumber(getNextFloorNumber(comment.getPostId()));
        Comment savedComment = commentRepository.save(comment);
        // 更新帖子评论数
        postService.increaseCommentCount(comment.getPostId());
        
        // 发送消息通知
        Long targetUserId = null;
        String messageContent = null;
        
        if (comment.getParentId() != null && comment.getParentId() > 0) {
            // 回复评论：通知父评论作者
            Optional<Comment> parentCommentOpt = commentRepository.findById(comment.getParentId());
            if (parentCommentOpt.isPresent()) {
                Comment parentComment = parentCommentOpt.get();
                if (!parentComment.getAuthorId().equals(comment.getAuthorId())) {
                    targetUserId = parentComment.getAuthorId();
                    messageContent = "您的评论收到了一条回复：" +
                        (savedComment.getContent().length() > 50 ? savedComment.getContent().substring(0, 50) + "..." : savedComment.getContent());
                }
            }
        } else {
            // 新评论：通知帖子作者
            Post post = postService.getPostById(comment.getPostId());
            if (post != null && !post.getAuthorId().equals(comment.getAuthorId())) {
                targetUserId = post.getAuthorId();
                messageContent = "您的帖子《" + post.getTitle() + "》收到了一条新评论：" +
                    (savedComment.getContent().length() > 50 ? savedComment.getContent().substring(0, 50) + "..." : savedComment.getContent());
            }
        }
        
        if (targetUserId != null && messageContent != null) {
            Message message = new Message();
            message.setUserId(targetUserId);
            message.setContent(messageContent);
            message.setIsRead(0);
            messageService.createMessage(message);
        }
        
        return savedComment;
    }

    @Override
    public Comment updateComment(Comment comment) {
        Optional<Comment> existingCommentOpt = commentRepository.findById(comment.getId());
        if (existingCommentOpt.isPresent()) {
            Comment existingComment = existingCommentOpt.get();
            existingComment.setContent(comment.getContent());
            existingComment.setStatus(comment.getStatus());
            existingComment.setUpdateTime(LocalDateTime.now());
            return commentRepository.save(existingComment);
        }
        return null;
    }

    @Override
    public boolean deleteComment(Long commentId) {
        Optional<Comment> commentOpt = commentRepository.findById(commentId);
        if (commentOpt.isPresent()) {
            Comment comment = commentOpt.get();
            comment.setIsDeleted(1);
            comment.setUpdateTime(LocalDateTime.now());
            commentRepository.save(comment);
            // 更新帖子评论数
            postService.decreaseCommentCount(comment.getPostId());
            return true;
        }
        return false;
    }

    @Override
    public Comment getCommentById(Long commentId) {
        Optional<Comment> commentOpt = commentRepository.findById(commentId);
        return commentOpt.orElse(null);
    }

    @Override
    public List<Comment> getCommentsByPostId(Long postId) {
        return commentRepository.findByPostIdAndIsDeletedOrderByFloorNumberAsc(postId, 0);
    }

    @Override
    public List<Comment> getCommentsByAuthorId(Long authorId) {
        return commentRepository.findByAuthorIdAndIsDeletedOrderByCreateTimeDesc(authorId, 0);
    }

    @Override
    public void increaseLikeCount(Long commentId) {
        Optional<Comment> commentOpt = commentRepository.findById(commentId);
        if (commentOpt.isPresent()) {
            Comment comment = commentOpt.get();
            comment.setLikeCount(comment.getLikeCount() + 1);
            commentRepository.save(comment);
        }
    }

    @Override
    public void decreaseLikeCount(Long commentId) {
        Optional<Comment> commentOpt = commentRepository.findById(commentId);
        if (commentOpt.isPresent()) {
            Comment comment = commentOpt.get();
            if (comment.getLikeCount() > 0) {
                comment.setLikeCount(comment.getLikeCount() - 1);
                commentRepository.save(comment);
            }
        }
    }

    @Override
    public Integer getNextFloorNumber(Long postId) {
        Integer maxFloor = commentRepository.findMaxFloorNumberByPostId(postId);
        return maxFloor == null ? 1 : maxFloor + 1;
    }
}