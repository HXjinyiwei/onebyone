package com.example.biyeshiji.controller;

import com.example.biyeshiji.common.PaginationResponse;
import com.example.biyeshiji.common.Response;
import com.example.biyeshiji.entity.Message;
import com.example.biyeshiji.entity.Novel;
import com.example.biyeshiji.entity.User;
import com.example.biyeshiji.repository.UserRepository;
import com.example.biyeshiji.service.MessageService;
import com.example.biyeshiji.service.NovelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/novel")
public class NovelController {

    @Autowired
    private NovelService novelService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private MessageService messageService;


    @PostMapping("/create")
    public Response<Novel> createNovel(@RequestBody Novel novel) {
        try {
            // 设置作者ID为当前登录用户
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = userRepository.findByUsername(auth.getName());
            if (currentUser == null) {
                return Response.error("用户未登录");
            }
            novel.setAuthorId(currentUser.getId());
            // 如果笔名为空，使用用户昵称
            if (novel.getPenName() == null || novel.getPenName().trim().isEmpty()) {
                novel.setPenName(currentUser.getNickname());
            }
            // 如果封面为空，设置默认封面
            if (novel.getCoverImage() == null || novel.getCoverImage().trim().isEmpty()) {
                novel.setCoverImage("/images/默认图片.jpg");
            }
            Novel createdNovel = novelService.createNovel(novel);
            if (createdNovel != null) {
                return Response.success("小说创建成功", createdNovel);
            } else {
                return Response.error("小说创建失败");
            }
        } catch (RuntimeException e) {
            return Response.error(e.getMessage());
        }
    }

    @RequestMapping(value = "/update", method = {RequestMethod.PUT, RequestMethod.POST})
    public Response<Novel> updateNovel(@RequestBody Novel novel) {
        try {
            // 权限检查：只有作者或管理员可以更新
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User currentUser = userRepository.findByUsername(auth.getName());
            if (currentUser == null) {
                return Response.error("用户未登录");
            }
            Novel existing = novelService.getNovelById(novel.getId());
            if (existing == null || existing.getIsDeleted() == 1) {
                return Response.error("小说不存在");
            }
            if (!currentUser.getId().equals(existing.getAuthorId()) && currentUser.getRole() != 1) {
                return Response.error("无权修改此小说");
            }
            // 如果笔名为空，使用用户昵称
            if (novel.getPenName() == null || novel.getPenName().trim().isEmpty()) {
                novel.setPenName(currentUser.getNickname());
            }
            // 如果封面为空，设置默认封面（保持原有封面不变，不覆盖）
            if (novel.getCoverImage() == null || novel.getCoverImage().trim().isEmpty()) {
                novel.setCoverImage(existing.getCoverImage());
            }
            Novel updatedNovel = novelService.updateNovel(novel);
            if (updatedNovel != null) {
                return Response.success("小说更新成功", updatedNovel);
            } else {
                return Response.error("小说更新失败");
            }
        } catch (RuntimeException e) {
            return Response.error(e.getMessage());
        }
    }

    @PostMapping("/delete/{id}")
    public Response<Void> deleteNovel(@PathVariable Long id) {
        // 权限检查
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userRepository.findByUsername(auth.getName());
        if (currentUser == null) {
            return Response.error("用户未登录");
        }
        Novel existing = novelService.getNovelById(id);
        if (existing == null) {
            return Response.error("小说不存在");
        }
        if (!currentUser.getId().equals(existing.getAuthorId()) && currentUser.getRole() != 1) {
            return Response.error("无权删除此小说");
        }
        boolean result = novelService.deleteNovel(id);
        if (result) {
            return Response.success("小说删除成功", null);
        } else {
            return Response.error("小说删除失败");
        }
    }

    @GetMapping("/detail/{id}")
    public ResponseEntity<?> getNovelById(@PathVariable Long id, HttpServletRequest request) {
        // 如果请求头Accept包含text/html，则重定向到HTML页面
        String acceptHeader = request.getHeader("Accept");
        if (acceptHeader != null && acceptHeader.contains("text/html")) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create("/novel/view/" + id))
                    .build();
        }
        Novel novel = novelService.getNovelById(id);
        if (novel == null || novel.getIsDeleted() == 1) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Response.error("小说不存在"));
        }
        // 权限检查：未审核小说只能被作者和管理员看到
        if (novel.getAuditStatus() != 1) { // 1=审核通过
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || auth.getName() == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Response.error("您无权查看此小说"));
            }
            User currentUser = userRepository.findByUsername(auth.getName());
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Response.error("您无权查看此小说"));
            }
            // 检查是否为作者或管理员
            boolean canView = currentUser.getId().equals(novel.getAuthorId()) || 
                             currentUser.getRole() == 1 || 
                             currentUser.getRole() == 2;
            if (!canView) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Response.error("您无权查看此小说"));
            }
        }
        // 增加浏览量
        novelService.increaseViewCount(id);
        return ResponseEntity.ok(Response.success("获取小说成功", novel));
    }

    @GetMapping("/all")
    public Response<PaginationResponse<Novel>> getAllNovels(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        PaginationResponse<Novel> novels = novelService.getNovelsWithFilterPagination(keyword, status, categoryId, page, pageSize);
        return Response.success("获取小说列表成功", novels);
    }

    @GetMapping("/author/{authorId}")
    public Response<List<Novel>> getNovelsByAuthorId(@PathVariable Long authorId) {
        List<Novel> novels = novelService.getNovelsByAuthorId(authorId);
        return Response.success("获取作者小说成功", novels);
    }

    // 带分页的作者小说查询
    @GetMapping("/author/{authorId}/page")
    public Response<PaginationResponse<Novel>> getNovelsByAuthorIdWithPagination(
            @PathVariable Long authorId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        PaginationResponse<Novel> novels = novelService.getNovelsByAuthorIdWithPagination(authorId, page, pageSize);
        return Response.success("获取作者小说成功", novels);
    }

    @GetMapping("/search")
    public Response<List<Novel>> searchNovels(@RequestParam(required = false) String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            // 如果关键词为空，返回所有小说（无分页）
            List<Novel> novels = novelService.getAllNovels();
            return Response.success("搜索成功", novels);
        }
        List<Novel> novels = novelService.searchNovels(keyword.trim());
        return Response.success("搜索成功", novels);
    }

    @GetMapping("/liked/{userId}")
    public Response<List<Novel>> getNovelsLikedByUser(@PathVariable Long userId) {
        List<Novel> novels = novelService.getNovelsLikedByUser(userId);
        return Response.success("获取用户点赞小说成功", novels);
    }

    @GetMapping("/liked/{userId}/page")
    public Response<PaginationResponse<Novel>> getNovelsLikedByUserWithPagination(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        PaginationResponse<Novel> novels = novelService.getNovelsLikedByUserWithPagination(userId, page, pageSize);
        return Response.success("获取用户点赞小说成功", novels);
    }

    @GetMapping("/favorited/{userId}")
    public Response<List<Novel>> getNovelsFavoritedByUser(@PathVariable Long userId) {
        List<Novel> novels = novelService.getNovelsFavoritedByUser(userId);
        return Response.success("获取用户收藏小说成功", novels);
    }

    @GetMapping("/favorited/{userId}/page")
    public Response<PaginationResponse<Novel>> getNovelsFavoritedByUserWithPagination(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        PaginationResponse<Novel> novels = novelService.getNovelsFavoritedByUserWithPagination(userId, page, pageSize);
        return Response.success("获取用户收藏小说成功", novels);
    }

    @GetMapping("/deleted/{authorId}")
    public Response<List<Novel>> getDeletedNovelsByAuthor(@PathVariable Long authorId) {
        // 权限检查：只有管理员或用户本人可以查看被删除的小说
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userRepository.findByUsername(auth.getName());
        if (currentUser == null) {
            return Response.error("未登录");
        }
        if (currentUser.getRole() != 1 && !currentUser.getId().equals(authorId)) {
            return Response.error("无权查看他人的被删除小说");
        }
        List<Novel> deletedNovels = novelService.getDeletedNovelsByAuthorId(authorId);
        return Response.success("获取被删除小说成功", deletedNovels);
    }

    @GetMapping("/deleted/{authorId}/page")
    public Response<PaginationResponse<Novel>> getDeletedNovelsByAuthorWithPagination(
            @PathVariable Long authorId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        // 权限检查：只有管理员或用户本人可以查看被删除的小说
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userRepository.findByUsername(auth.getName());
        if (currentUser == null) {
            return Response.error("未登录");
        }
        if (currentUser.getRole() != 1 && !currentUser.getId().equals(authorId)) {
            return Response.error("无权查看他人的被删除小说");
        }
        PaginationResponse<Novel> novels = novelService.getDeletedNovelsByAuthorIdWithPagination(authorId, page, pageSize);
        return Response.success("获取被删除小说成功", novels);
    }

    // 审核相关API（如果需要）
    @GetMapping("/pending")
    public Response<List<Novel>> getPendingNovels() {
        // 审核状态 0=待审核
        List<Novel> novels = novelService.getNovelsByAuditStatus(0);
        return Response.success("获取待审核小说成功", novels);
    }

    @PostMapping("/approve/{id}")
    public Response<Void> approveNovel(@PathVariable Long id) {
        boolean result = novelService.updateNovelAuditStatus(id, 1); // 1=审核通过
        if (result) {
            return Response.success("小说审核通过", null);
        } else {
            return Response.error("小说审核通过失败");
        }
    }

    @PostMapping("/reject/{id}")
    public Response<Void> rejectNovel(@PathVariable Long id) {
        boolean result = novelService.updateNovelAuditStatus(id, 2); // 2=审核拒绝
        if (result) {
            return Response.success("小说审核拒绝", null);
        } else {
            return Response.error("小说审核拒绝失败");
        }
    }

    @PostMapping("/reject-with-reason/{id}")
    public Response<Void> rejectNovelWithReason(@PathVariable Long id, @RequestBody Map<String, String> requestBody) {
        String reason = requestBody.get("reason");
        boolean result = novelService.updateNovelAuditStatusWithReason(id, 2, reason); // 2=审核拒绝
        if (result) {
            // 创建消息通知给作者
            Novel novel = novelService.getNovelById(id);
            if (novel != null) {
                Message message = new Message();
                message.setUserId(novel.getAuthorId());
                message.setContent("您的小说《" + novel.getTitle() + "》审核未通过。拒绝原因：" + (reason != null ? reason : "未提供具体原因") + "。请修改后重新提交。");
                messageService.createMessage(message);
            }
            return Response.success("小说审核拒绝，已通知作者", null);
        } else {
            return Response.error("小说审核拒绝失败");
        }
    }

    // 热门排序
    @GetMapping("/top/view")
    public Response<PaginationResponse<Novel>> getTopByView(@RequestParam(required = false, defaultValue = "10") Integer limit,
                                              @RequestParam(required = false) Long categoryId,
                                              @RequestParam(required = false, defaultValue = "1") Integer page,
                                              @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        PaginationResponse<Novel> novels;
        // 如果提供了limit参数，使用旧方法（兼容性）
        if (limit != null && limit > 0) {
            List<Novel> novelList = novelService.getNovelsOrderByViewCountDesc(limit, categoryId);
            novels = PaginationResponse.of(novelList, 1, limit, novelList.size());
        } else {
            // 使用分页方法
            novels = novelService.getNovelsOrderByViewCountDescWithPaginationResponse(page, pageSize, categoryId);
        }
        return Response.success("获取热门小说（浏览量）成功", novels);
    }

    @GetMapping("/top/like")
    public Response<PaginationResponse<Novel>> getTopByLike(@RequestParam(required = false, defaultValue = "10") Integer limit,
                                              @RequestParam(required = false) Long categoryId,
                                              @RequestParam(required = false, defaultValue = "1") Integer page,
                                              @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        PaginationResponse<Novel> novels;
        // 如果提供了limit参数，使用旧方法（兼容性）
        if (limit != null && limit > 0) {
            List<Novel> novelList = novelService.getNovelsOrderByLikeCountDesc(limit, categoryId);
            novels = PaginationResponse.of(novelList, 1, limit, novelList.size());
        } else {
            // 使用分页方法
            novels = novelService.getNovelsOrderByLikeCountDescWithPaginationResponse(page, pageSize, categoryId);
        }
        return Response.success("获取热门小说（点赞数）成功", novels);
    }

    @GetMapping("/top/favorite")
    public Response<PaginationResponse<Novel>> getTopByFavorite(@RequestParam(required = false, defaultValue = "10") Integer limit,
                                                  @RequestParam(required = false) Long categoryId,
                                                  @RequestParam(required = false, defaultValue = "1") Integer page,
                                                  @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        PaginationResponse<Novel> novels;
        // 如果提供了limit参数，使用旧方法（兼容性）
        if (limit != null && limit > 0) {
            List<Novel> novelList = novelService.getNovelsOrderByFavoriteCountDesc(limit, categoryId);
            novels = PaginationResponse.of(novelList, 1, limit, novelList.size());
        } else {
            // 使用分页方法
            novels = novelService.getNovelsOrderByFavoriteCountDescWithPaginationResponse(page, pageSize, categoryId);
        }
        return Response.success("获取热门小说（收藏数）成功", novels);
    }

    @GetMapping("/top/new")
    public Response<PaginationResponse<Novel>> getTopByNew(@RequestParam(required = false, defaultValue = "10") Integer limit,
                                             @RequestParam(required = false) Long categoryId,
                                             @RequestParam(required = false, defaultValue = "1") Integer page,
                                             @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        PaginationResponse<Novel> novels;
        // 如果提供了limit参数，使用旧方法（兼容性）
        if (limit != null && limit > 0) {
            List<Novel> novelList = novelService.getNovelsOrderByCreateTimeDesc(limit, categoryId);
            novels = PaginationResponse.of(novelList, 1, limit, novelList.size());
        } else {
            // 使用分页方法
            novels = novelService.getNovelsOrderByCreateTimeDescWithPaginationResponse(page, pageSize, categoryId);
        }
        return Response.success("获取最新小说成功", novels);
    }

    @GetMapping("/count")
    public Response<Long> countNovels() {
        long count = novelService.countNovels();
        return Response.success("获取小说总数成功", count);
    }

    // 封禁/解禁API
    @PostMapping("/ban/{id}")
    public Response<Void> banNovel(@PathVariable Long id, @RequestParam(required = false) String reason) {
        // 权限检查：管理员（角色1）和高级管理员（角色2）可以封禁
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userRepository.findByUsername(auth.getName());
        if (currentUser == null || (currentUser.getRole() != 1 && currentUser.getRole() != 2)) {
            return Response.error("只有管理员可以封禁小说");
        }
        
        boolean result = novelService.banNovel(id, reason);
        if (result) {
            return Response.success("小说封禁成功", null);
        } else {
            return Response.error("小说封禁失败");
        }
    }

    @PostMapping("/unban/{id}")
    public Response<Void> unbanNovel(@PathVariable Long id) {
        // 权限检查：管理员（角色1）和高级管理员（角色2）可以解禁
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userRepository.findByUsername(auth.getName());
        if (currentUser == null || (currentUser.getRole() != 1 && currentUser.getRole() != 2)) {
            return Response.error("只有管理员可以解禁小说");
        }
        
        boolean result = novelService.unbanNovel(id);
        if (result) {
            return Response.success("小说解禁成功", null);
        } else {
            return Response.error("小说解禁失败");
        }
    }

    @GetMapping("/banned")
    public Response<List<Novel>> getBannedNovels() {
        // 权限检查：只有管理员可以查看封禁小说列表
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User currentUser = userRepository.findByUsername(auth.getName());
        if (currentUser == null || currentUser.getRole() != 1) {
            return Response.error("只有管理员可以查看封禁小说列表");
        }
        
        List<Novel> novels = novelService.getBannedNovels();
        return Response.success("获取封禁小说列表成功", novels);
    }

    // 检查标题是否已存在
    @GetMapping("/check-title")
    public Response<Map<String, Object>> checkTitleExists(@RequestParam String title,
                                                          @RequestParam(required = false) Long excludeId) {
        if (title == null || title.trim().isEmpty()) {
            return Response.success("标题为空", Map.of("exists", false));
        }
        
        boolean exists = novelService.isTitleExists(title.trim(), excludeId);
        return Response.success("检查完成", Map.of("exists", exists));
    }

    // 重新提交小说（从拒绝状态改为待审核）
    @PostMapping("/resubmit/{id}")
    public Response<Void> resubmitNovel(@PathVariable Long id) {
        try {
            boolean result = novelService.resubmitNovel(id);
            if (result) {
                return Response.success("小说已重新提交，等待审核", null);
            } else {
                return Response.error("重新提交失败");
            }
        } catch (RuntimeException e) {
            return Response.error(e.getMessage());
        }
    }
}