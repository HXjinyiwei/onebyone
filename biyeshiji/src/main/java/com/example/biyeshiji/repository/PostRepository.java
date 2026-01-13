package com.example.biyeshiji.repository;

import com.example.biyeshiji.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    List<Post> findByStatus(Integer status);
    List<Post> findByAuthorId(Long authorId);
    List<Post> findByCategoryId(Long categoryId);
    List<Post> findByStatusAndIsDeleted(Integer status, Integer isDeleted);
    List<Post> findByAuthorIdAndIsDeleted(Long authorId, Integer isDeleted);
    List<Post> findByCategoryIdAndStatusAndIsDeleted(Long categoryId, Integer status, Integer isDeleted);
    List<Post> findByIsDeleted(Integer isDeleted);
    List<Post> findByCategoryIdAndIsDeleted(Long categoryId, Integer isDeleted);
    
    @Query("SELECT DISTINCT p FROM Post p LEFT JOIN FETCH p.author LEFT JOIN FETCH p.category WHERE p.isDeleted = 0")
    List<Post> findAllWithAuthorAndCategory();
    
    @Query("SELECT DISTINCT p FROM Post p LEFT JOIN FETCH p.author LEFT JOIN FETCH p.category WHERE p.id = :postId AND p.isDeleted = 0")
    Post findByIdWithAuthorAndCategory(@Param("postId") Long postId);
    
    @Query("SELECT DISTINCT p FROM Post p LEFT JOIN FETCH p.author LEFT JOIN FETCH p.category WHERE p.id IN :postIds AND p.isDeleted = 0")
    List<Post> findByIdsWithAuthorAndCategory(@Param("postIds") List<Long> postIds);
    
    @Query("SELECT DISTINCT p FROM Post p LEFT JOIN FETCH p.author a LEFT JOIN FETCH p.category WHERE p.isDeleted = 0 AND (p.title LIKE %:keyword% OR p.content LIKE %:keyword% OR a.nickname LIKE %:keyword%)")
    List<Post> searchByKeyword(@Param("keyword") String keyword);
    
    @Query("SELECT DISTINCT p FROM Post p LEFT JOIN FETCH p.author LEFT JOIN FETCH p.category WHERE p.authorId = :authorId AND p.isDeleted = 1")
    List<Post> findDeletedByAuthorId(@Param("authorId") Long authorId);

    // 统计未删除的帖子数量
    long countByIsDeleted(Integer isDeleted);
}
