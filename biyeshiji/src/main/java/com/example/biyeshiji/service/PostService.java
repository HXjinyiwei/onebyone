package com.example.biyeshiji.service;

import com.example.biyeshiji.entity.Post;

import java.util.List;

public interface PostService {
    Post createPost(Post post);
    Post updatePost(Post post);
    boolean deletePost(Long postId);
    Post getPostById(Long postId);
    List<Post> getAllPosts();
    List<Post> getPostsByCategoryId(Long categoryId);
    List<Post> getPostsWithFilter(Long categoryId, Integer status, Integer page, Integer pageSize);
    List<Post> getPostsByAuthorId(Long authorId);
    List<Post> getPostsLikedByUser(Long userId);
    List<Post> getPostsFavoritedByUser(Long userId);
    void increaseViewCount(Long postId);
    void increaseLikeCount(Long postId);
    void decreaseLikeCount(Long postId);
    void increaseCommentCount(Long postId);
    void decreaseCommentCount(Long postId);
    void increaseFavoriteCount(Long postId);
    void decreaseFavoriteCount(Long postId);
    boolean lockPost(Long postId);
    boolean unlockPost(Long postId);
    List<Post> getPostsByStatus(Integer status);
    boolean updatePostStatus(Long postId, Integer status);
    List<Post> searchPosts(String keyword);
    List<Post> searchPostsWithStatus(String keyword, Integer status);
    List<Post> getDeletedPostsByAuthorId(Long authorId);
    // 封禁/解禁功能
    boolean banPost(Long postId, String reason);
    boolean unbanPost(Long postId);
    List<Post> getBannedPosts();
    // 统计
    long countPosts();
}