package com.example.biyeshiji.service.impl;

import com.example.biyeshiji.entity.Chapter;
import com.example.biyeshiji.entity.Novel;
import com.example.biyeshiji.entity.User;
import com.example.biyeshiji.repository.ChapterRepository;
import com.example.biyeshiji.repository.NovelRepository;
import com.example.biyeshiji.repository.UserRepository;
import com.example.biyeshiji.service.ChapterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ChapterServiceImpl implements ChapterService {

    @Autowired
    private ChapterRepository chapterRepository;

    @Autowired
    private NovelRepository novelRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public Chapter createChapter(Chapter chapter) {
        // 获取当前登录用户
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth != null ? auth.getName() : null;
        User currentUser = username != null ? userRepository.findByUsername(username) : null;

        if (currentUser == null) {
            throw new RuntimeException("用户未登录");
        }

        // 验证小说是否存在且属于当前用户（或管理员）
        Optional<Novel> novelOpt = novelRepository.findById(chapter.getNovelId());
        if (novelOpt.isEmpty()) {
            throw new RuntimeException("小说不存在");
        }

        Novel novel = novelOpt.get();
        boolean isAuthor = novel.getAuthorId().equals(currentUser.getId());
        boolean isAdmin = currentUser.getRole() == 1;

        if (!isAuthor && !isAdmin) {
            throw new RuntimeException("无权为该小说添加章节");
        }

        // 自动设置排序号（如果没有提供）
        if (chapter.getSortOrder() == null) {
            Integer maxSortOrder = chapterRepository.findMaxSortOrderByNovelId(chapter.getNovelId());
            chapter.setSortOrder(maxSortOrder + 1);
        }

        // 自动计算字数（如果内容不为空）
        if (chapter.getContent() != null) {
            chapter.setWordCount(chapter.getContent().length());
        }

        Chapter savedChapter = chapterRepository.save(chapter);

        // 更新小说章节数
        updateChapterCount(chapter.getNovelId());

        return savedChapter;
    }

    @Override
    public Chapter updateChapter(Chapter chapter) {
        Optional<Chapter> existingChapterOpt = chapterRepository.findById(chapter.getId());
        if (existingChapterOpt.isPresent()) {
            Chapter existingChapter = existingChapterOpt.get();

            // 权限检查：只有作者或管理员可以修改
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth != null ? auth.getName() : null;
            User currentUser = username != null ? userRepository.findByUsername(username) : null;

            if (currentUser == null) {
                throw new RuntimeException("用户未登录");
            }

            // 验证小说权限
            Optional<Novel> novelOpt = novelRepository.findById(existingChapter.getNovelId());
            if (novelOpt.isEmpty()) {
                throw new RuntimeException("小说不存在");
            }

            Novel novel = novelOpt.get();
            boolean isAuthor = novel.getAuthorId().equals(currentUser.getId());
            boolean isAdmin = currentUser.getRole() == 1;

            if (!isAuthor && !isAdmin) {
                throw new RuntimeException("无权修改此章节");
            }

            // 更新字段
            existingChapter.setTitle(chapter.getTitle());
            existingChapter.setContent(chapter.getContent());
            existingChapter.setSortOrder(chapter.getSortOrder());
            existingChapter.setIsFree(chapter.getIsFree());
            existingChapter.setUpdateTime(LocalDateTime.now());

            // 重新计算字数
            if (chapter.getContent() != null) {
                existingChapter.setWordCount(chapter.getContent().length());
            }

            return chapterRepository.save(existingChapter);
        }
        return null;
    }

    @Override
    @Transactional
    public boolean deleteChapter(Long chapterId) {
        Optional<Chapter> chapterOpt = chapterRepository.findById(chapterId);
        if (chapterOpt.isPresent()) {
            Chapter chapter = chapterOpt.get();
            Long novelId = chapter.getNovelId();

            // 权限检查（略，实际应检查）
            chapterRepository.delete(chapter);

            // 更新小说章节数
            updateChapterCount(novelId);

            return true;
        }
        return false;
    }

    @Override
    @Transactional(readOnly = true)
    public Chapter getChapterById(Long chapterId) {
        Chapter chapter = chapterRepository.findByIdWithNovel(chapterId);
        if (chapter == null) {
            return null;
        }
        // 检查权限
        if (isAdminOrAuthor(chapter.getNovelId())) {
            return chapter;
        } else {
            // 普通用户只能查看审核通过的章节
            if (chapter.getAuditStatus() != null && chapter.getAuditStatus() == 1) {
                return chapter;
            } else {
                // 未审核或审核拒绝的章节对普通用户不可见
                return null;
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Chapter> getAllChapters() {
        // 获取当前用户
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            // 未登录用户只能查看审核通过的章节
            return chapterRepository.findByAuditStatusOrderByCreateTimeDesc(1);
        }
        
        String username = auth.getName();
        User currentUser = userRepository.findByUsername(username);
        if (currentUser == null) {
            return chapterRepository.findByAuditStatusOrderByCreateTimeDesc(1);
        }
        
        // 管理员和高级管理员可以查看所有章节（包含小说信息）
        if (currentUser.getRole() == 1 || currentUser.getRole() == 2) {
            return chapterRepository.findAllWithNovelByOrderByCreateTimeDesc();
        }
        
        // 普通用户只能查看审核通过的章节
        return chapterRepository.findByAuditStatusOrderByCreateTimeDesc(1);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Chapter> getChaptersByNovelId(Long novelId) {
        // 检查当前用户是否是管理员或作者
        if (isAdminOrAuthor(novelId)) {
            // 作者或管理员可以查看所有章节（包括未审核的）
            return chapterRepository.findByNovelIdOrderBySortOrderAsc(novelId);
        } else {
            // 普通用户只能查看审核通过的章节
            return chapterRepository.findByNovelIdAndAuditStatusOrderBySortOrderAsc(novelId, 1);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Chapter getPreviousChapter(Long novelId, Integer sortOrder) {
        // 获取对当前用户可见的章节列表
        List<Chapter> chapters = getChaptersByNovelId(novelId);
        Chapter previous = null;
        for (Chapter chapter : chapters) {
            if (chapter.getSortOrder() < sortOrder) {
                previous = chapter;
            } else {
                break;
            }
        }
        return previous;
    }

    @Override
    @Transactional(readOnly = true)
    public Chapter getNextChapter(Long novelId, Integer sortOrder) {
        // 获取对当前用户可见的章节列表
        List<Chapter> chapters = getChaptersByNovelId(novelId);
        for (Chapter chapter : chapters) {
            if (chapter.getSortOrder() > sortOrder) {
                return chapter;
            }
        }
        return null;
    }

    @Override
    public void increaseViewCount(Long chapterId) {
        Optional<Chapter> chapterOpt = chapterRepository.findById(chapterId);
        if (chapterOpt.isPresent()) {
            Chapter chapter = chapterOpt.get();
            chapter.setViewCount(chapter.getViewCount() + 1);
            chapterRepository.save(chapter);

            // 同时增加小说的浏览量
            Optional<Novel> novelOpt = novelRepository.findById(chapter.getNovelId());
            if (novelOpt.isPresent()) {
                Novel novel = novelOpt.get();
                novel.setViewCount(novel.getViewCount() + 1);
                novelRepository.save(novel);
            }
        }
    }

    @Override
    @Transactional
    public void updateChapterCount(Long novelId) {
        Long count = chapterRepository.countByNovelId(novelId);
        Optional<Novel> novelOpt = novelRepository.findById(novelId);
        if (novelOpt.isPresent()) {
            Novel novel = novelOpt.get();
            novel.setChapterCount(count.intValue());
            novelRepository.save(novel);
        }
    }

    /**
     * 检查当前用户是否是管理员或指定小说的作者
     */
    private boolean isAdminOrAuthor(Long novelId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        String username = auth.getName();
        User currentUser = userRepository.findByUsername(username);
        if (currentUser == null) return false;
        if (currentUser.getRole() == 1) return true; // 管理员
        Optional<Novel> novelOpt = novelRepository.findById(novelId);
        if (novelOpt.isPresent()) {
            Novel novel = novelOpt.get();
            return novel.getAuthorId().equals(currentUser.getId());
        }
        return false;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Chapter> getChaptersByAuditStatus(Integer auditStatus) {
        // 直接返回按审核状态筛选的章节
        return chapterRepository.findByAuditStatusOrderByCreateTimeDesc(auditStatus);
    }

    @Override
    @Transactional
    public boolean approveChapter(Long chapterId) {
        Optional<Chapter> chapterOpt = chapterRepository.findById(chapterId);
        if (chapterOpt.isPresent()) {
            Chapter chapter = chapterOpt.get();
            chapter.setAuditStatus(1); // 审核通过
            chapterRepository.save(chapter);
            return true;
        }
        return false;
    }

    @Override
    @Transactional
    public boolean rejectChapter(Long chapterId) {
        Optional<Chapter> chapterOpt = chapterRepository.findById(chapterId);
        if (chapterOpt.isPresent()) {
            Chapter chapter = chapterOpt.get();
            chapter.setAuditStatus(2); // 审核拒绝
            chapterRepository.save(chapter);
            return true;
        }
        return false;
    }

    @Override
    @Transactional
    public boolean banChapter(Long chapterId, String reason) {
        Optional<Chapter> chapterOpt = chapterRepository.findById(chapterId);
        if (chapterOpt.isPresent()) {
            Chapter chapter = chapterOpt.get();
            // 设置审核状态为封禁（3）
            chapter.setAuditStatus(3);
            chapter.setUpdateTime(LocalDateTime.now());
            chapterRepository.save(chapter);
            
            // 发送消息通知给小说作者
            // 需要导入MessageService和Message实体
            // 这里简化处理，实际应该发送消息
            return true;
        }
        return false;
    }

    @Override
    @Transactional
    public boolean unbanChapter(Long chapterId) {
        Optional<Chapter> chapterOpt = chapterRepository.findById(chapterId);
        if (chapterOpt.isPresent()) {
            Chapter chapter = chapterOpt.get();
            // 设置审核状态为审核通过（1）
            chapter.setAuditStatus(1);
            chapter.setUpdateTime(LocalDateTime.now());
            chapterRepository.save(chapter);
            return true;
        }
        return false;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Chapter> getBannedChapters() {
        // 获取所有章节，筛选出封禁状态的章节（audit_status=3）
        return chapterRepository.findByAuditStatusOrderByCreateTimeDesc(3);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Chapter> searchChapters(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            // 如果关键词为空，返回所有章节（根据用户权限）
            return getAllChapters();
        }
        
        String trimmedKeyword = keyword.trim();
        
        // 特殊关键词处理：如果搜索"封禁"，则返回审核状态为3的章节
        if ("封禁".equals(trimmedKeyword) || "banned".equalsIgnoreCase(trimmedKeyword)) {
            // 直接返回封禁章节
            return getBannedChapters();
        }
        
        // 执行搜索
        List<Chapter> chapters = chapterRepository.searchChapters(trimmedKeyword);
        // 根据用户权限过滤结果
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            // 未登录用户只能查看审核通过的章节
            return chapters.stream()
                    .filter(chapter -> chapter.getAuditStatus() != null && chapter.getAuditStatus() == 1)
                    .toList();
        }
        String username = auth.getName();
        User currentUser = userRepository.findByUsername(username);
        if (currentUser == null) {
            return chapters.stream()
                    .filter(chapter -> chapter.getAuditStatus() != null && chapter.getAuditStatus() == 1)
                    .toList();
        }
        // 管理员和高级管理员可以查看所有搜索结果
        if (currentUser.getRole() == 1 || currentUser.getRole() == 2) {
            return chapters;
        }
        // 普通用户只能查看审核通过的章节
        return chapters.stream()
                .filter(chapter -> chapter.getAuditStatus() != null && chapter.getAuditStatus() == 1)
                .toList();
    }
}