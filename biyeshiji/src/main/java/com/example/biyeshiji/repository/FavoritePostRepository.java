package com.example.biyeshiji.repository;

import com.example.biyeshiji.entity.FavoritePost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FavoritePostRepository extends JpaRepository<FavoritePost, Long> {
    FavoritePost findByUserIdAndPostId(Long userId, Long postId);
    void deleteByUserIdAndPostId(Long userId, Long postId);
    List<FavoritePost> findByUserId(Long userId);
}
