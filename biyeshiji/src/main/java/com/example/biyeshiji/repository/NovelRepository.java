package com.example.biyeshiji.repository;

import com.example.biyeshiji.entity.Novel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    @Query("SELECT DISTINCT n FROM Novel n LEFT JOIN FETCH n.author LEFT JOIN FETCH n.categories WHERE n.id = :novelId AND n.isDeleted = 0 AND n.auditStatus = 1")
    Novel findByIdWithAuthor(@Param("novelId") Long novelId);

    @Query("SELECT DISTINCT n FROM Novel n LEFT JOIN FETCH n.author LEFT JOIN FETCH n.categories WHERE n.id IN :novelIds AND n.isDeleted = 0 AND n.auditStatus = 1")
    List<Novel> findByIdsWithAuthor(@Param("novelIds") List<Long> novelIds);

    @Query("SELECT DISTINCT n FROM Novel n LEFT JOIN FETCH n.author a LEFT JOIN FETCH n.categories WHERE n.isDeleted = 0 AND (n.title LIKE %:keyword% OR n.description LIKE %:keyword% OR a.nickname LIKE %:keyword% OR n.penName LIKE %:keyword% OR EXISTS (SELECT 1 FROM n.categories c WHERE c.name LIKE %:keyword%))")
    List<Novel> searchByKeyword(@Param("keyword") String keyword);

    @Query("SELECT DISTINCT n FROM Novel n LEFT JOIN FETCH n.author LEFT JOIN FETCH n.categories WHERE n.authorId = :authorId AND n.isDeleted = 1")
    List<Novel> findDeletedByAuthorId(@Param("authorId") Long authorId);

    // 按浏览量降序排序（只显示审核通过的小说）
    @Query("SELECT n FROM Novel n WHERE n.isDeleted = 0 AND n.auditStatus = 1 ORDER BY n.viewCount DESC")
    List<Novel> findAllOrderByViewCountDesc();

    // 按点赞数降序排序（只显示审核通过的小说）
    @Query("SELECT n FROM Novel n WHERE n.isDeleted = 0 AND n.auditStatus = 1 ORDER BY n.likeCount DESC")
    List<Novel> findAllOrderByLikeCountDesc();

    // 按收藏数降序排序（只显示审核通过的小说）
    @Query("SELECT n FROM Novel n WHERE n.isDeleted = 0 AND n.auditStatus = 1 ORDER BY n.favoriteCount DESC")
    List<Novel> findAllOrderByFavoriteCountDesc();

    // 按更新时间降序排序（只显示审核通过的小说）
    @Query("SELECT n FROM Novel n WHERE n.isDeleted = 0 AND n.auditStatus = 1 ORDER BY n.updateTime DESC")
    List<Novel> findAllOrderByCreateTimeDesc();

    // 按分类和浏览量降序排序（只显示审核通过的小说）
    @Query("SELECT DISTINCT n FROM Novel n LEFT JOIN n.categories c WHERE n.isDeleted = 0 AND n.auditStatus = 1 AND (:categoryId IS NULL OR c.id = :categoryId) ORDER BY n.viewCount DESC")
    List<Novel> findAllOrderByViewCountDesc(@Param("categoryId") Long categoryId);

    // 按分类和点赞数降序排序（只显示审核通过的小说）
    @Query("SELECT DISTINCT n FROM Novel n LEFT JOIN n.categories c WHERE n.isDeleted = 0 AND n.auditStatus = 1 AND (:categoryId IS NULL OR c.id = :categoryId) ORDER BY n.likeCount DESC")
    List<Novel> findAllOrderByLikeCountDesc(@Param("categoryId") Long categoryId);

    // 按分类和收藏数降序排序（只显示审核通过的小说）
    @Query("SELECT DISTINCT n FROM Novel n LEFT JOIN n.categories c WHERE n.isDeleted = 0 AND n.auditStatus = 1 AND (:categoryId IS NULL OR c.id = :categoryId) ORDER BY n.favoriteCount DESC")
    List<Novel> findAllOrderByFavoriteCountDesc(@Param("categoryId") Long categoryId);

    // 按分类和更新时间降序排序（只显示审核通过的小说）
    @Query("SELECT DISTINCT n FROM Novel n LEFT JOIN n.categories c WHERE n.isDeleted = 0 AND n.auditStatus = 1 AND (:categoryId IS NULL OR c.id = :categoryId) ORDER BY n.updateTime DESC")
    List<Novel> findAllOrderByCreateTimeDesc(@Param("categoryId") Long categoryId);

    // 根据标题和删除状态查询小说
    List<Novel> findByTitleAndIsDeleted(String title, Integer isDeleted);

    // 统计未删除的小说数量
    long countByIsDeleted(Integer isDeleted);
    
    // 原生SQL更新浏览量，避免触发@PreUpdate
    @Modifying(clearAutomatically = true)
    @Query(value = "UPDATE novel SET view_count = view_count + 1 WHERE id = :novelId", nativeQuery = true)
    void increaseViewCount(@Param("novelId") Long novelId);
    
    // 原生SQL更新点赞数，避免触发@PreUpdate
    @Modifying(clearAutomatically = true)
    @Query(value = "UPDATE novel SET like_count = like_count + 1 WHERE id = :novelId", nativeQuery = true)
    void increaseLikeCount(@Param("novelId") Long novelId);
    
    // 原生SQL减少点赞数，避免触发@PreUpdate
    @Modifying(clearAutomatically = true)
    @Query(value = "UPDATE novel SET like_count = like_count - 1 WHERE id = :novelId AND like_count > 0", nativeQuery = true)
    void decreaseLikeCount(@Param("novelId") Long novelId);
    
    // 原生SQL更新收藏数，避免触发@PreUpdate
    @Modifying(clearAutomatically = true)
    @Query(value = "UPDATE novel SET favorite_count = favorite_count + 1 WHERE id = :novelId", nativeQuery = true)
    void increaseFavoriteCount(@Param("novelId") Long novelId);
    
    // 原生SQL减少收藏数，避免触发@PreUpdate
    @Modifying(clearAutomatically = true)
    @Query(value = "UPDATE novel SET favorite_count = favorite_count - 1 WHERE id = :novelId AND favorite_count > 0", nativeQuery = true)
    void decreaseFavoriteCount(@Param("novelId") Long novelId);

    // 按最新章节更新时间降序排序（只显示审核通过的小说）- 使用章节的最新更新时间
    // 注意：这个查询返回的update_time字段实际上是COALESCE(c.latest_chapter_update_time, n.update_time)
    @Query(value = "SELECT n.id, n.title, n.cover_image, n.description, n.author_id, n.pen_name, n.tags, n.category_id, " +
            "n.status, n.view_count, n.like_count, n.favorite_count, n.chapter_count, n.is_deleted, " +
            "n.create_time, COALESCE(c.latest_chapter_update_time, n.update_time) as update_time, " +
            "n.audit_status, n.reject_reason " +
            "FROM novel n " +
            "LEFT JOIN (SELECT novel_id, MAX(update_time) as latest_chapter_update_time FROM chapter GROUP BY novel_id) c " +
            "ON n.id = c.novel_id " +
            "WHERE n.is_deleted = 0 AND n.audit_status = 1 " +
            "ORDER BY COALESCE(c.latest_chapter_update_time, n.update_time) DESC", nativeQuery = true)
    List<Novel> findAllOrderByLatestChapterUpdateTimeDesc();

    // 按分类和最新章节更新时间降序排序（只显示审核通过的小说）- 使用章节的最新更新时间
    // 注意：这个查询返回的update_time字段实际上是COALESCE(c.latest_chapter_update_time, n.update_time)
    @Query(value = "SELECT DISTINCT n.id, n.title, n.cover_image, n.description, n.author_id, n.pen_name, n.tags, n.category_id, " +
            "n.status, n.view_count, n.like_count, n.favorite_count, n.chapter_count, n.is_deleted, " +
            "n.create_time, COALESCE(c.latest_chapter_update_time, n.update_time) as update_time, " +
            "n.audit_status, n.reject_reason " +
            "FROM novel n " +
            "LEFT JOIN novel_category nc ON n.id = nc.novel_id " +
            "LEFT JOIN (SELECT novel_id, MAX(update_time) as latest_chapter_update_time FROM chapter GROUP BY novel_id) c " +
            "ON n.id = c.novel_id " +
            "WHERE n.is_deleted = 0 AND n.audit_status = 1 " +
            "AND (:categoryId IS NULL OR nc.category_id = :categoryId) " +
            "ORDER BY COALESCE(c.latest_chapter_update_time, n.update_time) DESC", nativeQuery = true)
    List<Novel> findAllOrderByLatestChapterUpdateTimeDesc(@Param("categoryId") Long categoryId);
}