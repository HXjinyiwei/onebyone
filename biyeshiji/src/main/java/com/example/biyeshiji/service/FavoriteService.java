package com.example.biyeshiji.service;

public interface FavoriteService {
    // 旧方法（仅帖子）保持兼容
    boolean favoritePost(Long userId, Long postId);
    boolean unfavoritePost(Long userId, Long postId);
    boolean isFavorited(Long userId, Long postId);

    // 新通用方法
    boolean favorite(Long userId, Integer targetType, Long targetId);
    boolean unfavorite(Long userId, Integer targetType, Long targetId);
    boolean isFavorited(Long userId, Integer targetType, Long targetId);
}