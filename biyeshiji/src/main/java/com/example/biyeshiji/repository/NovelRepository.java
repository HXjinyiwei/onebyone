package com.example.biyeshiji.repository;

import com.example.biyeshiji.entity.Novel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NovelRepository extends JpaRepository<Novel, Long> {
    List<Novel> findByAuthorId(Long authorId);
    List<Novel> findByStatus(Integer status);
    List<Novel> findByAuthorIdAndIsDeleted(Long authorId, Integer isDeleted);
    List<Novel> findByIsDeleted(Integer isDeleted);
    List<Novel> findByStatusAndIsDeleted(Integer status, Integer isDeleted);

    @Query("SELECT DISTINCT n FROM Novel n LEFT JOIN FETCH n.author LEFT JOIN FETCH n.categories WHERE n.isDeleted = 0")
    List<Novel> findAllWithAuthor();

    @Query("SELECT DISTINCT n FROM Novel n LEFT JOIN FETCH n.author LEFT JOIN FETCH n.categories WHERE n.id = :novelId AND n.isDeleted = 0")
    Novel findByIdWithAuthor(@Param("novelId") Long novelId);

    @Query("SELECT DISTINCT n FROM Novel n LEFT JOIN FETCH n.author LEFT JOIN FETCH n.categories WHERE n.id IN :novelIds AND n.isDeleted = 0")
    List<Novel> findByIdsWithAuthor(@Param("novelIds") List<Long> novelIds);

    @Query("SELECT DISTINCT n FROM Novel n LEFT JOIN FETCH n.author a LEFT JOIN FETCH n.categories WHERE n.isDeleted = 0 AND (n.title LIKE %:keyword% OR n.description LIKE %:keyword% OR a.nickname LIKE %:keyword% OR n.penName LIKE %:keyword% OR EXISTS (SELECT 1 FROM n.categories c WHERE c.name LIKE %:keyword%))")
    List<Novel> searchByKeyword(@Param("keyword") String keyword);

    @Query("SELECT DISTINCT n FROM Novel n LEFT JOIN FETCH n.author LEFT JOIN FETCH n.categories WHERE n.authorId = :authorId AND n.isDeleted = 1")
    List<Novel> findDeletedByAuthorId(@Param("authorId") Long authorId);

    // 按浏览量降序排序
    @Query("SELECT n FROM Novel n WHERE n.isDeleted = 0 ORDER BY n.viewCount DESC")
    List<Novel> findAllOrderByViewCountDesc();

    // 按点赞数降序排序
    @Query("SELECT n FROM Novel n WHERE n.isDeleted = 0 ORDER BY n.likeCount DESC")
    List<Novel> findAllOrderByLikeCountDesc();

    // 按收藏数降序排序
    @Query("SELECT n FROM Novel n WHERE n.isDeleted = 0 ORDER BY n.favoriteCount DESC")
    List<Novel> findAllOrderByFavoriteCountDesc();

    // 按创建时间降序排序
    @Query("SELECT n FROM Novel n WHERE n.isDeleted = 0 ORDER BY n.createTime DESC")
    List<Novel> findAllOrderByCreateTimeDesc();

    // 按分类和浏览量降序排序
    @Query("SELECT DISTINCT n FROM Novel n LEFT JOIN n.categories c WHERE n.isDeleted = 0 AND (:categoryId IS NULL OR c.id = :categoryId) ORDER BY n.viewCount DESC")
    List<Novel> findAllOrderByViewCountDesc(@Param("categoryId") Long categoryId);

    // 按分类和点赞数降序排序
    @Query("SELECT DISTINCT n FROM Novel n LEFT JOIN n.categories c WHERE n.isDeleted = 0 AND (:categoryId IS NULL OR c.id = :categoryId) ORDER BY n.likeCount DESC")
    List<Novel> findAllOrderByLikeCountDesc(@Param("categoryId") Long categoryId);

    // 按分类和收藏数降序排序
    @Query("SELECT DISTINCT n FROM Novel n LEFT JOIN n.categories c WHERE n.isDeleted = 0 AND (:categoryId IS NULL OR c.id = :categoryId) ORDER BY n.favoriteCount DESC")
    List<Novel> findAllOrderByFavoriteCountDesc(@Param("categoryId") Long categoryId);

    // 按分类和创建时间降序排序
    @Query("SELECT DISTINCT n FROM Novel n LEFT JOIN n.categories c WHERE n.isDeleted = 0 AND (:categoryId IS NULL OR c.id = :categoryId) ORDER BY n.createTime DESC")
    List<Novel> findAllOrderByCreateTimeDesc(@Param("categoryId") Long categoryId);

    // 根据标题和删除状态查询小说
    List<Novel> findByTitleAndIsDeleted(String title, Integer isDeleted);

    // 统计未删除的小说数量
    long countByIsDeleted(Integer isDeleted);
}