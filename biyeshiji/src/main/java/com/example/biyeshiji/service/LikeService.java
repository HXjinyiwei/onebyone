package com.example.biyeshiji.service;

public interface LikeService {
    boolean like(Long userId, Integer targetType, Long targetId);
    boolean unlike(Long userId, Integer targetType, Long targetId);
    boolean isLiked(Long userId, Integer targetType, Long targetId);
}