package com.example.biyeshiji.service;

import com.example.biyeshiji.entity.Novel;

import java.util.List;

public interface NovelService {
    Novel createNovel(Novel novel);
    Novel updateNovel(Novel novel);
    boolean deleteNovel(Long novelId);
    Novel getNovelById(Long novelId);
    List<Novel> getAllNovels();
    List<Novel> getNovelsByAuthorId(Long authorId);
    List<Novel> getNovelsWithFilter(String keyword, Integer status, Long categoryId, Integer page, Integer pageSize);
    List<Novel> getNovelsLikedByUser(Long userId);
    List<Novel> getNovelsFavoritedByUser(Long userId);
    void increaseViewCount(Long novelId);
    void increaseLikeCount(Long novelId);
    void decreaseLikeCount(Long novelId);
    void increaseFavoriteCount(Long novelId);
    void decreaseFavoriteCount(Long novelId);
    boolean updateNovelStatus(Long novelId, Integer status);
    List<Novel> searchNovels(String keyword);
    List<Novel> getDeletedNovelsByAuthorId(Long authorId);
    List<Novel> getNovelsByStatus(Integer status);
    List<Novel> getNovelsOrderByViewCountDesc(Integer limit, Long categoryId);
    List<Novel> getNovelsOrderByLikeCountDesc(Integer limit, Long categoryId);
    List<Novel> getNovelsOrderByFavoriteCountDesc(Integer limit, Long categoryId);
    List<Novel> getNovelsOrderByCreateTimeDesc(Integer limit, Long categoryId);
    // 审核相关
    List<Novel> getNovelsByAuditStatus(Integer auditStatus);
    boolean updateNovelAuditStatus(Long novelId, Integer auditStatus);
    boolean updateNovelAuditStatusWithReason(Long novelId, Integer auditStatus, String rejectReason);
    // 封禁/解禁功能
    boolean banNovel(Long novelId, String reason);
    boolean unbanNovel(Long novelId);
    List<Novel> getBannedNovels();
    // 统计
    long countNovels();
    // 重名检查
    boolean isTitleExists(String title, Long excludeNovelId);
    
    // 重新提交（从拒绝状态改为审核中）
    boolean resubmitNovel(Long novelId);
}