package com.example.biyeshiji.repository;

import com.example.biyeshiji.entity.Chapter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChapterRepository extends JpaRepository<Chapter, Long> {
    List<Chapter> findByNovelId(Long novelId);
    List<Chapter> findByNovelIdOrderBySortOrderAsc(Long novelId);
    List<Chapter> findByNovelIdAndIsFree(Long novelId, Integer isFree);

    @Query("SELECT c FROM Chapter c LEFT JOIN FETCH c.novel WHERE c.novelId = :novelId ORDER BY c.sortOrder ASC")
    List<Chapter> findByNovelIdWithNovel(@Param("novelId") Long novelId);

    @Query("SELECT c FROM Chapter c LEFT JOIN FETCH c.novel WHERE c.id = :chapterId")
    Chapter findByIdWithNovel(@Param("chapterId") Long chapterId);

    @Query("SELECT c FROM Chapter c LEFT JOIN FETCH c.novel WHERE c.id IN :chapterIds")
    List<Chapter> findByIdsWithNovel(@Param("chapterIds") List<Long> chapterIds);

    // 获取小说的最大排序号
    @Query("SELECT COALESCE(MAX(c.sortOrder), 0) FROM Chapter c WHERE c.novelId = :novelId")
    Integer findMaxSortOrderByNovelId(@Param("novelId") Long novelId);

    // 根据小说ID和排序号获取章节
    Chapter findByNovelIdAndSortOrder(Long novelId, Integer sortOrder);

    // 根据小说ID和标题获取章节（用于唯一性检查）
    List<Chapter> findByNovelIdAndTitle(Long novelId, String title);

    // 统计小说章节数
    Long countByNovelId(Long novelId);

    // 根据小说ID和审核状态获取章节（按排序号升序）
    List<Chapter> findByNovelIdAndAuditStatusOrderBySortOrderAsc(Long novelId, Integer auditStatus);

    // 根据小说ID和审核状态获取章节（不排序）
    List<Chapter> findByNovelIdAndAuditStatus(Long novelId, Integer auditStatus);

    // 根据审核状态获取所有章节（按创建时间降序）
    List<Chapter> findByAuditStatusOrderByCreateTimeDesc(Integer auditStatus);
    
    // 获取所有章节（按创建时间降序）
    @Query("SELECT c FROM Chapter c ORDER BY c.createTime DESC")
    List<Chapter> findAllByOrderByCreateTimeDesc();
    
    // 获取所有章节（按创建时间降序），包含小说信息
    @Query("SELECT c FROM Chapter c LEFT JOIN FETCH c.novel ORDER BY c.createTime DESC")
    List<Chapter> findAllWithNovelByOrderByCreateTimeDesc();
    
    // 搜索章节（按标题、内容、小说标题模糊搜索）
    @Query("SELECT c FROM Chapter c LEFT JOIN FETCH c.novel n WHERE " +
           "LOWER(c.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(c.content) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(n.title) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "ORDER BY c.createTime DESC")
    List<Chapter> searchChapters(@Param("keyword") String keyword);
}