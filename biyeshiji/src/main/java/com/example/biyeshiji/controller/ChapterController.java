package com.example.biyeshiji.controller;

import com.example.biyeshiji.common.Response;
import com.example.biyeshiji.entity.Chapter;
import com.example.biyeshiji.entity.Novel;
import com.example.biyeshiji.entity.User;
import com.example.biyeshiji.repository.NovelRepository;
import com.example.biyeshiji.repository.UserRepository;
import com.example.biyeshiji.service.ChapterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/chapter")
public class ChapterController {

    @Autowired
    private ChapterService chapterService;

    @Autowired
    private NovelRepository novelRepository;

    @Autowired
    private UserRepository userRepository;


    @PostMapping("/create")
    public Response<Chapter> createChapter(@RequestBody Chapter chapter) {
        try {
            // 验证小说存在且用户有权限
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = userRepository.findByUsername(auth.getName());
            if (currentUser == null) {
                return Response.error("用户未登录");
            }
            Novel novel = novelRepository.findById(chapter.getNovelId()).orElse(null);
            if (novel == null || novel.getIsDeleted() == 1) {
                return Response.error("小说不存在");
            }
            // 检查权限：只有作者或管理员可以添加章节
            if (!currentUser.getId().equals(novel.getAuthorId()) && currentUser.getRole() != 1) {
                return Response.error("无权为此小说添加章节");
            }
            // 检查小说审核状态：未审核的小说不能发布章节
            if (novel.getAuditStatus() != 1) { // 1=审核通过
                return Response.error("未审核或已拒绝的小说不能发布章节");
            }
            Chapter createdChapter = chapterService.createChapter(chapter);
            if (createdChapter != null) {
                // 更新小说章节计数
                chapterService.updateChapterCount(novel.getId());
                return Response.success("章节创建成功", createdChapter);
            } else {
                return Response.error("章节创建失败");
            }
        } catch (RuntimeException e) {
            return Response.error(e.getMessage());
        }
    }

    @RequestMapping(value = "/update", method = {RequestMethod.PUT, RequestMethod.POST})
    public Response<Chapter> updateChapter(@RequestBody Chapter chapter) {
        try {
            // 权限检查
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = userRepository.findByUsername(auth.getName());
            if (currentUser == null) {
                return Response.error("用户未登录");
            }
            Chapter existing = chapterService.getChapterById(chapter.getId());
            if (existing == null) {
                return Response.error("章节不存在");
            }
            Novel novel = novelRepository.findById(existing.getNovelId()).orElse(null);
            if (novel == null || novel.getIsDeleted() == 1) {
                return Response.error("小说不存在");
            }
            if (!currentUser.getId().equals(novel.getAuthorId()) && currentUser.getRole() != 1) {
                return Response.error("无权修改此章节");
            }
            // 章节编辑后需要重新审核（设置审核状态为待审核）
            chapter.setAuditStatus(0); // 0=待审核
            Chapter updatedChapter = chapterService.updateChapter(chapter);
            if (updatedChapter != null) {
                return Response.success("章节更新成功，已重新提交审核", updatedChapter);
            } else {
                return Response.error("章节更新失败");
            }
        } catch (RuntimeException e) {
            return Response.error(e.getMessage());
        }
    }

    @PostMapping("/delete/{id}")
    public Response<Void> deleteChapter(@PathVariable Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userRepository.findByUsername(auth.getName());
        if (currentUser == null) {
            return Response.error("用户未登录");
        }
        Chapter existing = chapterService.getChapterById(id);
        if (existing == null) {
            return Response.error("章节不存在");
        }
        Novel novel = novelRepository.findById(existing.getNovelId()).orElse(null);
        if (novel == null || novel.getIsDeleted() == 1) {
            return Response.error("小说不存在");
        }
        if (!currentUser.getId().equals(novel.getAuthorId()) && currentUser.getRole() != 1) {
            return Response.error("无权删除此章节");
        }
        boolean result = chapterService.deleteChapter(id);
        if (result) {
            // 更新小说章节计数
            chapterService.updateChapterCount(novel.getId());
            return Response.success("章节删除成功", null);
        } else {
            return Response.error("章节删除失败");
        }
    }

    @GetMapping("/detail/{id}")
    public Response<Chapter> getChapterById(@PathVariable Long id) {
        Chapter chapter = chapterService.getChapterById(id);
        if (chapter == null) {
            return Response.error("章节不存在");
        }
        // 检查小说是否被删除
        Novel novel = novelRepository.findById(chapter.getNovelId()).orElse(null);
        if (novel == null || novel.getIsDeleted() == 1) {
            return Response.error("小说不存在");
        }
        
        // 权限检查：未审核章节只能被管理员和作者看到
        if (chapter.getAuditStatus() != 1) { // 1=审核通过
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || auth.getName() == null) {
                return Response.error("您无权查看此章节");
            }
            User currentUser = userRepository.findByUsername(auth.getName());
            if (currentUser == null) {
                return Response.error("您无权查看此章节");
            }
            // 检查是否为作者或管理员
            boolean canView = currentUser.getId().equals(novel.getAuthorId()) || 
                             currentUser.getRole() == 1 || 
                             currentUser.getRole() == 2;
            if (!canView) {
                return Response.error("您无权查看此章节");
            }
        }
        
        // 增加浏览量
        chapterService.increaseViewCount(id);
        return Response.success("获取章节成功", chapter);
    }

    @GetMapping("/list/{novelId}")
    public Response<List<Chapter>> getChaptersByNovelId(@PathVariable Long novelId) {
        Novel novel = novelRepository.findById(novelId).orElse(null);
        if (novel == null || novel.getIsDeleted() == 1) {
            return Response.error("小说不存在");
        }
        List<Chapter> chapters = chapterService.getChaptersByNovelId(novelId);
        return Response.success("获取章节列表成功", chapters);
    }

    @GetMapping("/previous/{novelId}/{sortOrder}")
    public Response<Chapter> getPreviousChapter(@PathVariable Long novelId, @PathVariable Integer sortOrder) {
        Chapter chapter = chapterService.getPreviousChapter(novelId, sortOrder);
        if (chapter == null) {
            return Response.success("没有上一章", null);
        }
        return Response.success("获取上一章成功", chapter);
    }

    @GetMapping("/next/{novelId}/{sortOrder}")
    public Response<Chapter> getNextChapter(@PathVariable Long novelId, @PathVariable Integer sortOrder) {
        Chapter chapter = chapterService.getNextChapter(novelId, sortOrder);
        if (chapter == null) {
            return Response.success("没有下一章", null);
        }
        return Response.success("获取下一章成功", chapter);
    }

    // 获取待审核章节列表（仅管理员和高级管理员）
    @GetMapping("/pending")
    public Response<List<Chapter>> getPendingChapters() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userRepository.findByUsername(auth.getName());
        if (currentUser == null || (currentUser.getRole() != 1 && currentUser.getRole() != 2)) {
            return Response.error("无权访问");
        }
        List<Chapter> chapters = chapterService.getChaptersByAuditStatus(0);
        return Response.success("获取待审核章节成功", chapters);
    }

    // 审核通过章节（仅管理员和高级管理员）
    @PostMapping("/approve/{id}")
    public Response<Void> approveChapter(@PathVariable Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userRepository.findByUsername(auth.getName());
        if (currentUser == null || (currentUser.getRole() != 1 && currentUser.getRole() != 2)) {
            return Response.error("无权操作");
        }
        boolean result = chapterService.approveChapter(id);
        if (result) {
            return Response.success("章节审核通过", null);
        } else {
            return Response.error("章节审核失败");
        }
    }

    // 审核拒绝章节（仅管理员和高级管理员）
    @PostMapping("/reject/{id}")
    public Response<Void> rejectChapter(@PathVariable Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userRepository.findByUsername(auth.getName());
        if (currentUser == null || (currentUser.getRole() != 1 && currentUser.getRole() != 2)) {
            return Response.error("无权操作");
        }
        boolean result = chapterService.rejectChapter(id);
        if (result) {
            return Response.success("章节审核拒绝", null);
        } else {
            return Response.error("章节审核失败");
        }
    }

    // 封禁/解禁API
    @PostMapping("/ban/{id}")
    public Response<Void> banChapter(@PathVariable Long id, @RequestParam(required = false) String reason) {
        // 权限检查：管理员（角色1）和高级管理员（角色2）可以封禁
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userRepository.findByUsername(auth.getName());
        if (currentUser == null || (currentUser.getRole() != 1 && currentUser.getRole() != 2)) {
            return Response.error("只有管理员可以封禁章节");
        }
        
        boolean result = chapterService.banChapter(id, reason);
        if (result) {
            return Response.success("章节封禁成功", null);
        } else {
            return Response.error("章节封禁失败");
        }
    }

    @PostMapping("/unban/{id}")
    public Response<Void> unbanChapter(@PathVariable Long id) {
        // 权限检查：管理员（角色1）和高级管理员（角色2）可以解禁
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userRepository.findByUsername(auth.getName());
        if (currentUser == null || (currentUser.getRole() != 1 && currentUser.getRole() != 2)) {
            return Response.error("只有管理员可以解禁章节");
        }
        
        boolean result = chapterService.unbanChapter(id);
        if (result) {
            return Response.success("章节解禁成功", null);
        } else {
            return Response.error("章节解禁失败");
        }
    }

    @GetMapping("/banned")
    public Response<List<Chapter>> getBannedChapters() {
        // 权限检查：管理员（角色1）和高级管理员（角色2）可以查看封禁章节列表
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userRepository.findByUsername(auth.getName());
        if (currentUser == null || (currentUser.getRole() != 1 && currentUser.getRole() != 2)) {
            return Response.error("只有管理员可以查看封禁章节列表");
        }
        
        List<Chapter> chapters = chapterService.getBannedChapters();
        return Response.success("获取封禁章节列表成功", chapters);
    }
    
    @GetMapping("/all")
    public Response<List<Chapter>> getAllChapters(
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String search) {
        // 权限检查：管理员（角色1）和高级管理员（角色2）可以查看所有章节
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userRepository.findByUsername(auth.getName());
        if (currentUser == null || (currentUser.getRole() != 1 && currentUser.getRole() != 2)) {
            return Response.error("只有管理员可以查看所有章节");
        }
        
        List<Chapter> chapters;
        if (search != null && !search.trim().isEmpty()) {
            // 使用搜索功能
            chapters = chapterService.searchChapters(search.trim());
        } else if (status != null) {
            // 根据状态筛选
            chapters = chapterService.getChaptersByAuditStatus(status);
        } else {
            // 获取所有章节
            chapters = chapterService.getAllChapters();
        }
        return Response.success("获取章节列表成功", chapters);
    }
}