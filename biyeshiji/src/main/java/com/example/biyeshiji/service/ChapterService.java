package com.example.biyeshiji.service;

import com.example.biyeshiji.entity.Chapter;

import java.util.List;

public interface ChapterService {
    Chapter createChapter(Chapter chapter);
    Chapter updateChapter(Chapter chapter);
    boolean deleteChapter(Long chapterId);
    Chapter getChapterById(Long chapterId);
    List<Chapter> getAllChapters();
    List<Chapter> getChaptersByNovelId(Long novelId);
    Chapter getPreviousChapter(Long novelId, Integer sortOrder);
    Chapter getNextChapter(Long novelId, Integer sortOrder);
    void increaseViewCount(Long chapterId);
    void updateChapterCount(Long novelId);
    
    // 审核相关方法
    List<Chapter> getChaptersByAuditStatus(Integer auditStatus);
    boolean approveChapter(Long chapterId);
    boolean rejectChapter(Long chapterId);
    
    // 封禁/解禁功能
    boolean banChapter(Long chapterId, String reason);
    boolean unbanChapter(Long chapterId);
    List<Chapter> getBannedChapters();
    
    // 搜索功能
    List<Chapter> searchChapters(String keyword);
}