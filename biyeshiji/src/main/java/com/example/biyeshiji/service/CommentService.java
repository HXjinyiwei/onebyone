package com.example.biyeshiji.service;

import com.example.biyeshiji.entity.Comment;

import java.util.List;

public interface CommentService {
    Comment createComment(Comment comment);
    Comment updateComment(Comment comment);
    boolean deleteComment(Long commentId);
    Comment getCommentById(Long commentId);
    List<Comment> getCommentsByPostId(Long postId);
    List<Comment> getCommentsByAuthorId(Long authorId);
    void increaseLikeCount(Long commentId);
    void decreaseLikeCount(Long commentId);
    Integer getNextFloorNumber(Long postId);
}