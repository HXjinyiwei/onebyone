package com.example.biyeshiji.service.impl;

import com.example.biyeshiji.common.PaginationResponse;
import com.example.biyeshiji.entity.*;
import com.example.biyeshiji.repository.*;
import com.example.biyeshiji.service.MessageService;
import com.example.biyeshiji.service.NovelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class NovelServiceImpl implements NovelService {

    @Autowired
    private NovelRepository novelRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LikeRecordRepository likeRecordRepository;

    @Autowired
    private FavoritePostRepository favoritePostRepository; // 暂时复用，后续可能需要新建FavoriteNovelRepository

    @Autowired
    private FavoriteRecordRepository favoriteRecordRepository;

    @Autowired
    private MessageService messageService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Override
    public Novel createNovel(Novel novel) {
        // 获取当前登录用户
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth != null ? auth.getName() : null;
        User currentUser = username != null ? userRepository.findByUsername(username) : null;

        if (currentUser == null) {
            throw new RuntimeException("用户未登录");
        }

        // 检查小说标题是否已存在
        if (novel.getTitle() != null && !novel.getTitle().trim().isEmpty()) {
            boolean titleExists = isTitleExists(novel.getTitle().trim(), null);
            if (titleExists) {
                throw new RuntimeException("小说标题已存在，请使用其他标题");
            }
        }

        // 设置作者ID
        novel.setAuthorId(currentUser.getId());

        // 如果笔名为空，使用用户昵称
        if (novel.getPenName() == null || novel.getPenName().trim().isEmpty()) {
            novel.setPenName(currentUser.getNickname());
        }

        // 默认状态为连载中（0）
        if (novel.getStatus() == null) {
            novel.setStatus(0);
        }

        // 处理分类：根据 categoryIds 设置 categories
        syncCategoriesFromIds(novel);

        return novelRepository.save(novel);
    }

    @Override
    public Novel updateNovel(Novel novel) {
        Optional<Novel> existingNovelOpt = novelRepository.findById(novel.getId());
        if (existingNovelOpt.isPresent()) {
            Novel existingNovel = existingNovelOpt.get();

            // 权限检查：只有作者或管理员可以修改
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth != null ? auth.getName() : null;
            User currentUser = username != null ? userRepository.findByUsername(username) : null;

            if (currentUser == null) {
                throw new RuntimeException("用户未登录");
            }

            boolean isAuthor = existingNovel.getAuthorId().equals(currentUser.getId());
            boolean isAdmin = currentUser.getRole() == 1;

            if (!isAuthor && !isAdmin) {
                throw new RuntimeException("无权修改此小说");
            }

            // 检查小说标题是否已存在（排除当前小说）
            if (novel.getTitle() != null && !novel.getTitle().trim().isEmpty() &&
                !novel.getTitle().trim().equals(existingNovel.getTitle())) {
                boolean titleExists = isTitleExists(novel.getTitle().trim(), novel.getId());
                if (titleExists) {
                    throw new RuntimeException("小说标题已存在，请使用其他标题");
                }
            }

            // 更新字段
            existingNovel.setTitle(novel.getTitle());
            // 封面：仅当新封面不为空时才更新
            if (novel.getCoverImage() != null) {
                existingNovel.setCoverImage(novel.getCoverImage());
            }
            existingNovel.setDescription(novel.getDescription());
            existingNovel.setPenName(novel.getPenName());
            existingNovel.setStatus(novel.getStatus());
            existingNovel.setUpdateTime(LocalDateTime.now());

            // 处理分类：根据 categoryIds 更新 categories
            if (novel.getCategoryIds() != null) {
                syncCategoriesFromIds(novel);
                // 将 novel 中的 categories 复制到 existingNovel
                existingNovel.getCategories().clear();
                existingNovel.getCategories().addAll(novel.getCategories());
            } else if (novel.getCategories() != null) {
                // 如果直接提供了 categories 集合，则使用它
                existingNovel.getCategories().clear();
                existingNovel.getCategories().addAll(novel.getCategories());
            }

            return novelRepository.save(existingNovel);
        }
        return null;
    }

    private void syncCategoriesFromIds(Novel novel) {
        if (novel.getCategoryIds() != null) {
            Set<Category> categories = novel.getCategoryIds().stream()
                    .map(categoryId -> categoryRepository.findById(categoryId).orElse(null))
                    .filter(category -> category != null)
                    .collect(Collectors.toSet());
            novel.setCategories(categories);
        }
    }

    @Override
    @Transactional
    public boolean deleteNovel(Long novelId) {
        Optional<Novel> novelOpt = novelRepository.findById(novelId);
        if (novelOpt.isPresent()) {
            Novel novel = novelOpt.get();
            novel.setIsDeleted(1);
            novel.setUpdateTime(LocalDateTime.now());
            novelRepository.save(novel);

            // 发送消息通知给小说作者
            Long authorId = novel.getAuthorId();
            String content = "您的小说《" + novel.getTitle() + "》已被删除。";
            Message message = new Message();
            message.setUserId(authorId);
            message.setContent(content);
            message.setCreateTime(LocalDateTime.now());
            message.setIsRead(0);
            messageService.createMessage(message);

            return true;
        }
        return false;
    }

    @Override
    @Transactional(readOnly = true)
    public Novel getNovelById(Long novelId) {
        Novel novel = novelRepository.findByIdWithAuthor(novelId);
        if (novel != null && novel.getCategories() != null) {
            novel.setCategoryIds(novel.getCategories().stream()
                    .map(Category::getId)
                    .collect(java.util.stream.Collectors.toSet()));
        }
        return novel;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Novel> getAllNovels() {
        // 获取当前登录用户
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth != null ? auth.getName() : null;
        User currentUser = username != null ? userRepository.findByUsername(username) : null;

        // 所有未删除的小说，同时加载作者信息
        List<Novel> allNovels = novelRepository.findAllWithAuthor();

        List<Novel> filteredNovels;

        // 如果是管理员（角色1或2），返回所有小说（包括封禁的）
        if (currentUser != null && (currentUser.getRole() == 1 || currentUser.getRole() == 2)) {
            // 管理员可以看到所有小说，不需要过滤
            filteredNovels = allNovels;
        } else {
            // 普通用户或未登录用户，只能看到已审核通过的小说或者自己的小说
            final Long currentUserId = currentUser != null ? currentUser.getId() : null;
            filteredNovels = allNovels.stream()
                    .filter(novel -> Integer.valueOf(1).equals(novel.getAuditStatus()) ||
                            (currentUserId != null && novel.getAuthorId().equals(currentUserId)))
                    .toList();
        }

        // 按照创建时间倒序排序
        filteredNovels = new java.util.ArrayList<>(filteredNovels);
        filteredNovels.sort((n1, n2) -> n2.getCreateTime().compareTo(n1.getCreateTime()));

        return filteredNovels;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Novel> getNovelsByAuthorId(Long authorId) {
        // 获取所有未删除的小说，同时加载作者信息
        List<Novel> allNovels = novelRepository.findAllWithAuthor();

        // 筛选出指定作者的小说
        return allNovels.stream()
                .filter(novel -> novel.getAuthorId().equals(authorId))
                .sorted((n1, n2) -> n2.getCreateTime().compareTo(n1.getCreateTime()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Novel> getNovelsWithFilter(String keyword, Integer status, Long categoryId, Integer page, Integer pageSize) {
        PaginationResponse<Novel> response = getNovelsWithFilterPagination(keyword, status, categoryId, page, pageSize);
        return response.getData();
    }

    @Override
    @Transactional(readOnly = true)
    public PaginationResponse<Novel> getNovelsWithFilterPagination(String keyword, Integer status, Long categoryId, Integer page, Integer pageSize) {
        // 获取当前登录用户
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth != null ? auth.getName() : null;
        User currentUser = username != null ? userRepository.findByUsername(username) : null;

        List<Novel> filteredNovels;

        // 如果有搜索关键词，使用仓库的搜索方法
        if (keyword != null && !keyword.trim().isEmpty()) {
            // 使用仓库搜索方法（包含笔名字段）
            filteredNovels = novelRepository.searchByKeyword(keyword.trim());
        } else {
            // 没有搜索关键词，获取所有未删除的小说
            filteredNovels = novelRepository.findAllWithAuthor();
        }

        // 权限过滤
        List<Novel> permissionFilteredNovels;
        if (currentUser != null && (currentUser.getRole() == 1 || currentUser.getRole() == 2)) {
            // 管理员（角色1或2），返回所有小说（包括封禁的）
            permissionFilteredNovels = filteredNovels;
        } else {
            // 普通用户或未登录用户，只能看到已审核通过的小说或者自己的小说
            final Long currentUserId = currentUser != null ? currentUser.getId() : null;
            permissionFilteredNovels = filteredNovels.stream()
                    .filter(novel -> Integer.valueOf(1).equals(novel.getAuditStatus()) ||
                            (currentUserId != null && novel.getAuthorId().equals(currentUserId)))
                    .toList();
        }

        // 状态过滤（使用审核状态 auditStatus）
        if (status != null) {
            permissionFilteredNovels = permissionFilteredNovels.stream()
                    .filter(novel -> novel.getAuditStatus() != null && novel.getAuditStatus().equals(status))
                    .toList();
        }

        // 分类过滤
        if (categoryId != null && categoryId != 0) {
            permissionFilteredNovels = permissionFilteredNovels.stream()
                    .filter(novel -> novel.getCategories().stream()
                            .anyMatch(category -> category.getId().equals(categoryId)))
                    .toList();
        }

        // 按照创建时间倒序排序
        permissionFilteredNovels = new java.util.ArrayList<>(permissionFilteredNovels);
        permissionFilteredNovels.sort((n1, n2) -> n2.getCreateTime().compareTo(n1.getCreateTime()));

        // 验证分页参数
        if (page == null || page < 1) {
            page = 1;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 10;
        }

        // 分页处理
        int start = (page - 1) * pageSize;
        int end = Math.min(start + pageSize, permissionFilteredNovels.size());
        if (start >= end) {
            return PaginationResponse.of(List.of(), page, pageSize, permissionFilteredNovels.size());
        }

        List<Novel> pagedData = permissionFilteredNovels.subList(start, end);
        return PaginationResponse.of(pagedData, page, pageSize, permissionFilteredNovels.size());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Novel> getNovelsLikedByUser(Long userId) {
        // 查询用户点赞的小说记录（target_type=3 表示小说）
        List<LikeRecord> likeRecords = likeRecordRepository.findByUserIdAndTargetType(userId, 3);

        // 提取小说ID列表
        List<Long> novelIds = likeRecords.stream()
                .map(LikeRecord::getTargetId)
                .collect(Collectors.toList());

        // 如果没有点赞的小说，返回空列表
        if (novelIds.isEmpty()) {
            return new java.util.ArrayList<>();
        }

        // 查询并返回小说列表，使用fetch join加载作者信息
        List<Novel> novels = novelRepository.findByIdsWithAuthor(novelIds);

        // 按照创建时间倒序排序
        novels.sort((n1, n2) -> n2.getCreateTime().compareTo(n1.getCreateTime()));

        return novels;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Novel> getNovelsFavoritedByUser(Long userId) {
        // 查询用户收藏的小说记录（从favorite_record表查询，target_type=3 表示小说）
        List<FavoriteRecord> favoriteRecords = favoriteRecordRepository.findByUserIdAndTargetType(userId, 3);
        
        // 提取小说ID列表
        List<Long> novelIds = favoriteRecords.stream()
                .map(FavoriteRecord::getTargetId)
                .collect(Collectors.toList());
        
        // 如果没有收藏的小说，返回空列表
        if (novelIds.isEmpty()) {
            return new java.util.ArrayList<>();
        }
        
        // 查询并返回小说列表，使用fetch join加载作者信息
        List<Novel> novels = novelRepository.findByIdsWithAuthor(novelIds);
        
        // 按照创建时间倒序排序
        novels.sort((n1, n2) -> n2.getCreateTime().compareTo(n1.getCreateTime()));
        
        return novels;
    }

    @Override
    @Transactional
    public void increaseViewCount(Long novelId) {
        // 使用原生SQL更新浏览量，避免触发@PreUpdate
        novelRepository.increaseViewCount(novelId);
    }

    @Override
    @Transactional
    public void increaseLikeCount(Long novelId) {
        // 使用原生SQL更新点赞数，避免触发@PreUpdate
        novelRepository.increaseLikeCount(novelId);
    }

    @Override
    @Transactional
    public void decreaseLikeCount(Long novelId) {
        // 使用原生SQL减少点赞数，避免触发@PreUpdate
        novelRepository.decreaseLikeCount(novelId);
    }

    @Override
    @Transactional
    public void increaseFavoriteCount(Long novelId) {
        // 使用原生SQL更新收藏数，避免触发@PreUpdate
        novelRepository.increaseFavoriteCount(novelId);
    }

    @Override
    @Transactional
    public void decreaseFavoriteCount(Long novelId) {
        // 使用原生SQL减少收藏数，避免触发@PreUpdate
        novelRepository.decreaseFavoriteCount(novelId);
    }

    @Override
    public boolean updateNovelStatus(Long novelId, Integer status) {
        Optional<Novel> novelOpt = novelRepository.findById(novelId);
        if (novelOpt.isPresent()) {
            Novel novel = novelOpt.get();
            novel.setStatus(status);
            novel.setUpdateTime(LocalDateTime.now());
            novelRepository.save(novel);
            return true;
        }
        return false;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Novel> searchNovels(String keyword) {
        // 使用仓库方法搜索小说（包含未删除的小说）
        List<Novel> allNovels = novelRepository.searchByKeyword(keyword);

        // 获取当前登录用户
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth != null ? auth.getName() : null;
        User currentUser = username != null ? userRepository.findByUsername(username) : null;

        List<Novel> filteredNovels;

        // 如果是管理员（角色1或2），返回所有搜索结果（包括封禁的）
        if (currentUser != null && (currentUser.getRole() == 1 || currentUser.getRole() == 2)) {
            filteredNovels = new java.util.ArrayList<>(allNovels);
        } else {
            // 普通用户或未登录用户，只能看到已审核通过的小说或者自己的小说
            final Long currentUserId = currentUser != null ? currentUser.getId() : null;
            filteredNovels = allNovels.stream()
                    .filter(novel -> Integer.valueOf(1).equals(novel.getAuditStatus()) ||
                            (currentUserId != null && novel.getAuthorId().equals(currentUserId)))
                    .collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));
        }

        // 按照创建时间倒序排序
        filteredNovels.sort((n1, n2) -> n2.getCreateTime().compareTo(n1.getCreateTime()));

        return filteredNovels;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Novel> getDeletedNovelsByAuthorId(Long authorId) {
        // 直接调用仓库方法查询已删除的小说
        List<Novel> deletedNovels = novelRepository.findDeletedByAuthorId(authorId);
        // 按删除时间（或更新时间）倒序排序
        deletedNovels.sort((n1, n2) -> n2.getUpdateTime().compareTo(n1.getUpdateTime()));
        return deletedNovels;
    }

    @Override
    @Transactional(readOnly = true)
    public PaginationResponse<Novel> getNovelsByAuthorIdWithPagination(Long authorId, Integer page, Integer pageSize) {
        // 获取所有未删除的小说，同时加载作者信息
        List<Novel> allNovels = novelRepository.findAllWithAuthor();
        
        // 筛选出指定作者的小说
        List<Novel> filteredNovels = allNovels.stream()
                .filter(novel -> novel.getAuthorId().equals(authorId))
                .sorted((n1, n2) -> n2.getCreateTime().compareTo(n1.getCreateTime()))
                .toList();
        
        // 验证分页参数
        if (page == null || page < 1) {
            page = 1;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 10;
        }
        
        // 计算总数
        long total = filteredNovels.size();
        
        // 分页处理
        int start = (page - 1) * pageSize;
        int end = Math.min(start + pageSize, filteredNovels.size());
        if (start >= end) {
            return PaginationResponse.of(List.of(), page, pageSize, total);
        }
        
        List<Novel> content = filteredNovels.subList(start, end);
        return PaginationResponse.of(content, page, pageSize, total);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginationResponse<Novel> getNovelsLikedByUserWithPagination(Long userId, Integer page, Integer pageSize) {
        // 查询用户点赞的小说记录（target_type=3 表示小说）
        List<LikeRecord> likeRecords = likeRecordRepository.findByUserIdAndTargetType(userId, 3);
        
        // 提取小说 ID 列表
        List<Long> novelIds = likeRecords.stream()
                .map(LikeRecord::getTargetId)
                .collect(Collectors.toList());
        
        // 如果没有点赞的小说，返回空分页
        if (novelIds.isEmpty()) {
            return PaginationResponse.of(List.of(), page != null ? page : 1, pageSize != null ? pageSize : 10, 0);
        }
        
        // 查询并返回小说列表，使用 fetch join 加载作者信息
        List<Novel> allNovels = novelRepository.findByIdsWithAuthor(novelIds);
        
        // 按照创建时间倒序排序
        List<Novel> filteredNovels = new java.util.ArrayList<>(allNovels);
        filteredNovels.sort((n1, n2) -> n2.getCreateTime().compareTo(n1.getCreateTime()));
        
        // 验证分页参数
        if (page == null || page < 1) {
            page = 1;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 10;
        }
        
        // 计算总数
        long total = filteredNovels.size();
        
        // 分页处理
        int start = (page - 1) * pageSize;
        int end = Math.min(start + pageSize, filteredNovels.size());
        if (start >= end) {
            return PaginationResponse.of(List.of(), page, pageSize, total);
        }
        
        List<Novel> content = filteredNovels.subList(start, end);
        return PaginationResponse.of(content, page, pageSize, total);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginationResponse<Novel> getNovelsFavoritedByUserWithPagination(Long userId, Integer page, Integer pageSize) {
        // 查询用户收藏的小说记录（从 favorite_record 表查询，target_type=3 表示小说）
        List<FavoriteRecord> favoriteRecords = favoriteRecordRepository.findByUserIdAndTargetType(userId, 3);
        
        // 提取小说 ID 列表
        List<Long> novelIds = favoriteRecords.stream()
                .map(FavoriteRecord::getTargetId)
                .collect(Collectors.toList());
        
        // 如果没有收藏的小说，返回空分页
        if (novelIds.isEmpty()) {
            return PaginationResponse.of(List.of(), page != null ? page : 1, pageSize != null ? pageSize : 10, 0);
        }
        
        // 查询并返回小说列表，使用 fetch join 加载作者信息
        List<Novel> allNovels = novelRepository.findByIdsWithAuthor(novelIds);
        
        // 按照创建时间倒序排序
        List<Novel> filteredNovels = new java.util.ArrayList<>(allNovels);
        filteredNovels.sort((n1, n2) -> n2.getCreateTime().compareTo(n1.getCreateTime()));
        
        // 验证分页参数
        if (page == null || page < 1) {
            page = 1;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 10;
        }
        
        // 计算总数
        long total = filteredNovels.size();
        
        // 分页处理
        int start = (page - 1) * pageSize;
        int end = Math.min(start + pageSize, filteredNovels.size());
        if (start >= end) {
            return PaginationResponse.of(List.of(), page, pageSize, total);
        }
        
        List<Novel> content = filteredNovels.subList(start, end);
        return PaginationResponse.of(content, page, pageSize, total);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginationResponse<Novel> getDeletedNovelsByAuthorIdWithPagination(Long authorId, Integer page, Integer pageSize) {
        // 直接调用仓库方法查询已删除的小说
        List<Novel> deletedNovels = novelRepository.findDeletedByAuthorId(authorId);
        // 按删除时间（或更新时间）倒序排序
        deletedNovels.sort((n1, n2) -> n2.getUpdateTime().compareTo(n1.getUpdateTime()));
        
        // 验证分页参数
        if (page == null || page < 1) {
            page = 1;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 10;
        }
        
        // 计算总数
        long total = deletedNovels.size();
        
        // 分页处理
        int start = (page - 1) * pageSize;
        int end = Math.min(start + pageSize, deletedNovels.size());
        if (start >= end) {
            return PaginationResponse.of(List.of(), page, pageSize, total);
        }
        
        List<Novel> content = deletedNovels.subList(start, end);
        return PaginationResponse.of(content, page, pageSize, total);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Novel> getNovelsByStatus(Integer status) {
        // 获取所有未删除的小说，同时加载作者信息
        List<Novel> allNovels = novelRepository.findAllWithAuthor();

        // 筛选出指定状态的小说
        return allNovels.stream()
                .filter(novel -> novel.getStatus().equals(status))
                .sorted((n1, n2) -> n2.getCreateTime().compareTo(n1.getCreateTime()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Novel> getNovelsOrderByViewCountDesc(Integer limit, Long categoryId) {
        List<Novel> novels = novelRepository.findAllOrderByViewCountDesc(categoryId);
        if (limit != null && limit > 0 && novels.size() > limit) {
            novels = novels.subList(0, limit);
        }
        return novels;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Novel> getNovelsOrderByViewCountDescWithPagination(Integer page, Integer pageSize, Long categoryId) {
        List<Novel> novels = novelRepository.findAllOrderByViewCountDesc(categoryId);
        // 分页处理
        if (page == null || page < 1) {
            page = 1;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 10;
        }
        int start = (page - 1) * pageSize;
        int end = Math.min(start + pageSize, novels.size());
        if (start >= end) {
            return List.of();
        }
        return novels.subList(start, end);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Novel> getNovelsOrderByLikeCountDesc(Integer limit, Long categoryId) {
        List<Novel> novels = novelRepository.findAllOrderByLikeCountDesc(categoryId);
        if (limit != null && limit > 0 && novels.size() > limit) {
            novels = novels.subList(0, limit);
        }
        return novels;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Novel> getNovelsOrderByLikeCountDescWithPagination(Integer page, Integer pageSize, Long categoryId) {
        List<Novel> novels = novelRepository.findAllOrderByLikeCountDesc(categoryId);
        // 分页处理
        if (page == null || page < 1) {
            page = 1;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 10;
        }
        int start = (page - 1) * pageSize;
        int end = Math.min(start + pageSize, novels.size());
        if (start >= end) {
            return List.of();
        }
        return novels.subList(start, end);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Novel> getNovelsOrderByFavoriteCountDesc(Integer limit, Long categoryId) {
        List<Novel> novels = novelRepository.findAllOrderByFavoriteCountDesc(categoryId);
        if (limit != null && limit > 0 && novels.size() > limit) {
            novels = novels.subList(0, limit);
        }
        return novels;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Novel> getNovelsOrderByFavoriteCountDescWithPagination(Integer page, Integer pageSize, Long categoryId) {
        List<Novel> novels = novelRepository.findAllOrderByFavoriteCountDesc(categoryId);
        // 分页处理
        if (page == null || page < 1) {
            page = 1;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 10;
        }
        int start = (page - 1) * pageSize;
        int end = Math.min(start + pageSize, novels.size());
        if (start >= end) {
            return List.of();
        }
        return novels.subList(start, end);
    }

    // 按最新章节更新时间降序排序（只显示审核通过的小说）- 使用章节的最新更新时间
    @Override
    @Transactional(readOnly = true)
    public List<Novel> getNovelsOrderByCreateTimeDesc(Integer limit, Long categoryId) {
        List<Novel> novels = novelRepository.findAllOrderByLatestChapterUpdateTimeDesc(categoryId);
        // 手动加载关联数据（author和categories）
        loadAssociationsForNovels(novels);
        if (limit != null && limit > 0 && novels.size() > limit) {
            novels = novels.subList(0, limit);
        }
        return novels;
    }

    // 按最新章节更新时间降序排序（只显示审核通过的小说）- 使用章节的最新更新时间
    @Override
    @Transactional(readOnly = true)
    public List<Novel> getNovelsOrderByCreateTimeDescWithPagination(Integer page, Integer pageSize, Long categoryId) {
        List<Novel> novels = novelRepository.findAllOrderByLatestChapterUpdateTimeDesc(categoryId);
        // 手动加载关联数据（author和categories）
        loadAssociationsForNovels(novels);
        // 分页处理
        if (page == null || page < 1) {
            page = 1;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 10;
        }
        int start = (page - 1) * pageSize;
        int end = Math.min(start + pageSize, novels.size());
        if (start >= end) {
            return List.of();
        }
        return novels.subList(start, end);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Novel> getNovelsByAuditStatus(Integer auditStatus) {
        // 获取所有未删除的小说，同时加载作者信息
        List<Novel> allNovels = novelRepository.findAllWithAuthor();
        // 筛选出指定审核状态的小说
        return allNovels.stream()
                .filter(novel -> auditStatus.equals(novel.getAuditStatus()))
                .sorted((n1, n2) -> n2.getCreateTime().compareTo(n1.getCreateTime()))
                .toList();
    }

    @Override
    public boolean updateNovelAuditStatus(Long novelId, Integer auditStatus) {
        Optional<Novel> novelOpt = novelRepository.findById(novelId);
        if (novelOpt.isPresent()) {
            Novel novel = novelOpt.get();
            novel.setAuditStatus(auditStatus);
            novel.setUpdateTime(LocalDateTime.now());
            // 如果是审核拒绝（状态2），清空拒绝原因（因为旧方法不支持设置原因）
            if (auditStatus == 2) {
                novel.setRejectReason(null);
            }
            novelRepository.save(novel);
            return true;
        }
        return false;
    }

    @Override
    public boolean updateNovelAuditStatusWithReason(Long novelId, Integer auditStatus, String rejectReason) {
        Optional<Novel> novelOpt = novelRepository.findById(novelId);
        if (novelOpt.isPresent()) {
            Novel novel = novelOpt.get();
            novel.setAuditStatus(auditStatus);
            novel.setUpdateTime(LocalDateTime.now());
            // 如果是审核拒绝（状态2），设置拒绝原因
            if (auditStatus == 2) {
                novel.setRejectReason(rejectReason);
            } else {
                // 其他状态清空拒绝原因
                novel.setRejectReason(null);
            }
            novelRepository.save(novel);
            return true;
        }
        return false;
    }

    @Override
    public long countNovels() {
        return novelRepository.countByIsDeleted(0);
    }

    @Override
    @Transactional
    public boolean banNovel(Long novelId, String reason) {
        Optional<Novel> novelOpt = novelRepository.findById(novelId);
        if (novelOpt.isPresent()) {
            Novel novel = novelOpt.get();
            // 设置审核状态为封禁（3）
            novel.setAuditStatus(3);
            novel.setUpdateTime(LocalDateTime.now());
            novelRepository.save(novel);
            
            // 发送消息通知给小说作者
            Long authorId = novel.getAuthorId();
            String content = "您的小说《" + novel.getTitle() + "》已被封禁。";
            if (reason != null && !reason.isEmpty()) {
                content += " 原因：" + reason;
            }
            Message message = new Message();
            message.setUserId(authorId);
            message.setContent(content);
            message.setCreateTime(LocalDateTime.now());
            message.setIsRead(0);
            messageService.createMessage(message);
            
            return true;
        }
        return false;
    }

    @Override
    @Transactional
    public boolean unbanNovel(Long novelId) {
        Optional<Novel> novelOpt = novelRepository.findById(novelId);
        if (novelOpt.isPresent()) {
            Novel novel = novelOpt.get();
            // 设置审核状态为审核通过（1）
            novel.setAuditStatus(1);
            novel.setUpdateTime(LocalDateTime.now());
            novelRepository.save(novel);
            
            // 发送消息通知给小说作者
            Long authorId = novel.getAuthorId();
            String content = "您的小说《" + novel.getTitle() + "》已解禁，现在可以正常显示。";
            Message message = new Message();
            message.setUserId(authorId);
            message.setContent(content);
            message.setCreateTime(LocalDateTime.now());
            message.setIsRead(0);
            messageService.createMessage(message);
            
            return true;
        }
        return false;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Novel> getBannedNovels() {
        // 获取所有未删除的小说，同时加载作者信息
        List<Novel> allNovels = novelRepository.findAllWithAuthor();
        
        // 筛选出封禁状态的小说（audit_status=3）
        return allNovels.stream()
                .filter(novel -> novel.getAuditStatus() != null && novel.getAuditStatus() == 3)
                .sorted((n1, n2) -> n2.getUpdateTime().compareTo(n1.getUpdateTime()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isTitleExists(String title, Long excludeNovelId) {
        if (title == null || title.trim().isEmpty()) {
            return false;
        }
        
        // 查询是否存在相同标题的小说（排除已删除的）
        List<Novel> existingNovels = novelRepository.findByTitleAndIsDeleted(title.trim(), 0);
        
        if (excludeNovelId != null) {
            // 排除当前正在编辑的小说
            existingNovels = existingNovels.stream()
                    .filter(novel -> !novel.getId().equals(excludeNovelId))
                    .toList();
        }
        
        return !existingNovels.isEmpty();
    }
    
    @Override
    @Transactional
    public boolean resubmitNovel(Long novelId) {
        Optional<Novel> novelOpt = novelRepository.findById(novelId);
        if (novelOpt.isPresent()) {
            Novel novel = novelOpt.get();
            // 检查当前状态是否为已拒绝（2）
            if (novel.getAuditStatus() != null && novel.getAuditStatus() == 2) {
                // 将状态改为待审核（0），清空拒绝原因
                novel.setAuditStatus(0);
                novel.setRejectReason(null);
                novel.setUpdateTime(LocalDateTime.now());
                novelRepository.save(novel);
                
                // 发送消息通知给小说作者
                Long authorId = novel.getAuthorId();
                String content = "您的小说《" + novel.getTitle() + "》已重新提交，等待审核。";
                Message message = new Message();
                message.setUserId(authorId);
                message.setContent(content);
                message.setCreateTime(LocalDateTime.now());
                message.setIsRead(0);
                messageService.createMessage(message);
                
                return true;
            } else {
                throw new RuntimeException("只有被拒绝的小说才能重新提交");
            }
        }
        return false;
    }

    @Override
    @Transactional(readOnly = true)
    public PaginationResponse<Novel> getNovelsOrderByViewCountDescWithPaginationResponse(Integer page, Integer pageSize, Long categoryId) {
        List<Novel> novels = novelRepository.findAllOrderByViewCountDesc(categoryId);
        // 分页处理
        if (page == null || page < 1) {
            page = 1;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 10;
        }
        int start = (page - 1) * pageSize;
        int end = Math.min(start + pageSize, novels.size());
        if (start >= end) {
            return PaginationResponse.of(List.of(), page, pageSize, novels.size());
        }
        List<Novel> pagedData = novels.subList(start, end);
        return PaginationResponse.of(pagedData, page, pageSize, novels.size());
    }

    @Override
    @Transactional(readOnly = true)
    public PaginationResponse<Novel> getNovelsOrderByLikeCountDescWithPaginationResponse(Integer page, Integer pageSize, Long categoryId) {
        List<Novel> novels = novelRepository.findAllOrderByLikeCountDesc(categoryId);
        // 分页处理
        if (page == null || page < 1) {
            page = 1;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 10;
        }
        int start = (page - 1) * pageSize;
        int end = Math.min(start + pageSize, novels.size());
        if (start >= end) {
            return PaginationResponse.of(List.of(), page, pageSize, novels.size());
        }
        List<Novel> pagedData = novels.subList(start, end);
        return PaginationResponse.of(pagedData, page, pageSize, novels.size());
    }

    @Override
    @Transactional(readOnly = true)
    public PaginationResponse<Novel> getNovelsOrderByFavoriteCountDescWithPaginationResponse(Integer page, Integer pageSize, Long categoryId) {
        List<Novel> novels = novelRepository.findAllOrderByFavoriteCountDesc(categoryId);
        // 分页处理
        if (page == null || page < 1) {
            page = 1;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 10;
        }
        int start = (page - 1) * pageSize;
        int end = Math.min(start + pageSize, novels.size());
        if (start >= end) {
            return PaginationResponse.of(List.of(), page, pageSize, novels.size());
        }
        List<Novel> pagedData = novels.subList(start, end);
        return PaginationResponse.of(pagedData, page, pageSize, novels.size());
    }

    @Override
    @Transactional(readOnly = true)
    public PaginationResponse<Novel> getNovelsOrderByCreateTimeDescWithPaginationResponse(Integer page, Integer pageSize, Long categoryId) {
        List<Novel> novels = novelRepository.findAllOrderByLatestChapterUpdateTimeDesc(categoryId);
        // 手动加载关联数据（author和categories）
        loadAssociationsForNovels(novels);
        // 分页处理
        if (page == null || page < 1) {
            page = 1;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 10;
        }
        int start = (page - 1) * pageSize;
        int end = Math.min(start + pageSize, novels.size());
        if (start >= end) {
            return PaginationResponse.of(List.of(), page, pageSize, novels.size());
        }
        List<Novel> pagedData = novels.subList(start, end);
        return PaginationResponse.of(pagedData, page, pageSize, novels.size());
    }
    
    // 手动加载小说的关联数据（author和categories）
    private void loadAssociationsForNovels(List<Novel> novels) {
        if (novels == null || novels.isEmpty()) {
            return;
        }
        
        // 提取小说ID列表
        List<Long> novelIds = novels.stream()
                .map(Novel::getId)
                .collect(Collectors.toList());
        
        // 批量查询完整的小说数据（包含关联）
        List<Novel> fullNovels = novelRepository.findByIdsWithAuthor(novelIds);
        
        // 创建ID到完整小说的映射
        Map<Long, Novel> fullNovelMap = fullNovels.stream()
                .collect(Collectors.toMap(Novel::getId, novel -> novel));
        
        // 将关联数据复制到原始小说对象中
        for (Novel novel : novels) {
            Novel fullNovel = fullNovelMap.get(novel.getId());
            if (fullNovel != null) {
                // 复制关联数据
                novel.setAuthor(fullNovel.getAuthor());
                novel.setCategories(fullNovel.getCategories());
                // 触发@PostLoad以设置categoryIds
                novel.postLoad();
            }
        }
    }
}