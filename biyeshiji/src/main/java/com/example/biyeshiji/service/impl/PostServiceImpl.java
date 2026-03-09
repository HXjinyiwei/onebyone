package com.example.biyeshiji.service.impl;

import com.example.biyeshiji.entity.FavoritePost;
import com.example.biyeshiji.entity.FavoriteRecord;
import com.example.biyeshiji.entity.LikeRecord;
import com.example.biyeshiji.entity.Message;
import com.example.biyeshiji.entity.Post;
import com.example.biyeshiji.entity.User;
import com.example.biyeshiji.repository.FavoritePostRepository;
import com.example.biyeshiji.repository.FavoriteRecordRepository;
import com.example.biyeshiji.repository.LikeRecordRepository;
import com.example.biyeshiji.repository.MessageRepository;
import com.example.biyeshiji.repository.PostRepository;
import com.example.biyeshiji.repository.UserRepository;
import com.example.biyeshiji.service.MessageService;
import com.example.biyeshiji.service.PostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PostServiceImpl implements PostService {

    @Autowired
    private PostRepository postRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private LikeRecordRepository likeRecordRepository;
    
    @Autowired
    private FavoritePostRepository favoritePostRepository;

    @Autowired
    private FavoriteRecordRepository favoriteRecordRepository;

    @Autowired
    private MessageService messageService;

    @Override
    public Post createPost(Post post) {
        // 获取当前登录用户
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth != null ? auth.getName() : null;
        User currentUser = username != null ? userRepository.findByUsername(username) : null;
        
        // 如果是公告分类（categoryId=1），只有管理员可以发布
        if (post.getCategoryId() != null && post.getCategoryId() == 1L) {
            if (currentUser == null || currentUser.getRole() != 1) {
                throw new RuntimeException("只有管理员才能发布公告");
            }
            // 公告自动置顶
            post.setIsTop(1);
        } else {
            // 非公告帖子不能置顶
            post.setIsTop(0);
        }
        
        // 设置作者ID
        if (currentUser != null) {
            post.setAuthorId(currentUser.getId());
        }
        
        return postRepository.save(post);
    }

    @Override
    public Post updatePost(Post post) {
        Optional<Post> existingPostOpt = postRepository.findById(post.getId());
        if (existingPostOpt.isPresent()) {
            Post existingPost = existingPostOpt.get();
            
            // 获取当前登录用户
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String username = auth != null ? auth.getName() : null;
            User currentUser = username != null ? userRepository.findByUsername(username) : null;
            
            // 原帖子是公告，只有管理员可以修改
            if (existingPost.getCategoryId() != null && existingPost.getCategoryId() == 1L) {
                if (currentUser == null || currentUser.getRole() != 1) {
                    throw new RuntimeException("只有管理员才能修改公告");
                }
            }
            
            // 如果要修改为公告分类，只有管理员可以操作
            if (post.getCategoryId() != null && post.getCategoryId() == 1L && existingPost.getCategoryId() != 1L) {
                if (currentUser == null || currentUser.getRole() != 1) {
                    throw new RuntimeException("只有管理员才能发布公告");
                }
                // 公告自动置顶
                post.setIsTop(1);
            } else if (post.getCategoryId() != null && post.getCategoryId() != 1L && existingPost.getCategoryId() == 1L) {
                // 从公告修改为其他分类，取消置顶
                post.setIsTop(0);
            } else if (post.getCategoryId() != null && post.getCategoryId() != 1L) {
                // 非公告帖子不能置顶
                post.setIsTop(0);
            }
            
            existingPost.setTitle(post.getTitle());
            existingPost.setContent(post.getContent());
            existingPost.setCategoryId(post.getCategoryId());
            existingPost.setStatus(post.getStatus());
            existingPost.setIsLocked(post.getIsLocked());
            existingPost.setIsTop(post.getIsTop());
            existingPost.setUpdateTime(LocalDateTime.now());
            return postRepository.save(existingPost);
        }
        return null;
    }

    @Override
    @Transactional
    public boolean deletePost(Long postId) {
        Optional<Post> postOpt = postRepository.findById(postId);
        if (postOpt.isPresent()) {
            Post post = postOpt.get();
            post.setIsDeleted(1);
            post.setUpdateTime(LocalDateTime.now());
            postRepository.save(post);

            // 发送消息通知给帖子作者
            Long authorId = post.getAuthorId();
            String content = "您的帖子《" + post.getTitle() + "》已被删除。";
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
    public Post getPostById(Long postId) {
        return postRepository.findByIdWithAuthorAndCategory(postId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Post> getAllPosts() {
        // 获取当前登录用户
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth != null ? auth.getName() : null;
        User currentUser = username != null ? userRepository.findByUsername(username) : null;
        
        // 所有未删除的帖子，同时加载作者和分类信息
        List<Post> allPosts = postRepository.findAllWithAuthorAndCategory();
        
        List<Post> filteredPosts;
        
        // 如果是管理员（角色1或2），返回所有未封禁的帖子（status != 2）
        if (currentUser != null && (currentUser.getRole() == 1 || currentUser.getRole() == 2)) {
            final Long currentUserId = currentUser.getId();
            filteredPosts = allPosts.stream()
                    .filter(post -> !Integer.valueOf(2).equals(post.getStatus()) ||
                            (currentUserId != null && post.getAuthorId().equals(currentUserId)))
                    .toList();
        } else {
            // 普通用户或未登录用户，只能看到已审核通过的帖子或者自己的帖子
            final Long currentUserId = currentUser != null ? currentUser.getId() : null;
            filteredPosts = allPosts.stream()
                    .filter(post -> Integer.valueOf(0).equals(post.getStatus()) ||
                            (currentUserId != null && post.getAuthorId().equals(currentUserId)))
                    .toList();
        }
        
        // 按照置顶状态和创建时间排序，置顶帖子优先，同状态下按创建时间倒序
        filteredPosts = new java.util.ArrayList<>(filteredPosts);
        filteredPosts.sort((p1, p2) -> {
            if (!p1.getIsTop().equals(p2.getIsTop())) {
                return p2.getIsTop() - p1.getIsTop();
            }
            return p2.getCreateTime().compareTo(p1.getCreateTime());
        });
        
        return filteredPosts;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Post> getPostsByCategoryId(Long categoryId) {
        // 获取当前登录用户
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth != null ? auth.getName() : null;
        User currentUser = username != null ? userRepository.findByUsername(username) : null;
        
        // 获取所有未删除的帖子，同时加载作者和分类信息
        List<Post> allPosts = postRepository.findAllWithAuthorAndCategory();
        
        List<Post> filteredPosts;
        
        // 如果是管理员（角色1或2），返回所有未封禁的帖子（status != 2）
        if (currentUser != null && (currentUser.getRole() == 1 || currentUser.getRole() == 2)) {
            final Long currentUserId = currentUser.getId();
            filteredPosts = allPosts.stream()
                    .filter(post -> Objects.equals(post.getCategoryId(), categoryId))
                    .filter(post -> !Integer.valueOf(2).equals(post.getStatus()) ||
                            (currentUserId != null && post.getAuthorId().equals(currentUserId)))
                    .toList();
        } else {
            // 普通用户或未登录用户，只能看到已审核通过的帖子或者自己的帖子
            final Long currentUserId = currentUser != null ? currentUser.getId() : null;
            filteredPosts = allPosts.stream()
                    .filter(post -> Objects.equals(post.getCategoryId(), categoryId))
                    .filter(post -> Integer.valueOf(0).equals(post.getStatus()) ||
                            (currentUserId != null && post.getAuthorId().equals(currentUserId)))
                    .toList();
        }
        
        // 按照置顶状态和创建时间排序，置顶帖子优先，同状态下按创建时间倒序
        filteredPosts = new java.util.ArrayList<>(filteredPosts);
        filteredPosts.sort((p1, p2) -> {
            if (!p1.getIsTop().equals(p2.getIsTop())) {
                return p2.getIsTop() - p1.getIsTop();
            }
            return p2.getCreateTime().compareTo(p1.getCreateTime());
        });
        
        return filteredPosts;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Post> getPostsByAuthorId(Long authorId) {
        // 获取所有未删除的帖子，同时加载作者和分类信息
        List<Post> allPosts = postRepository.findAllWithAuthorAndCategory();
        
        // 筛选出指定作者的帖子
        return allPosts.stream()
                .filter(post -> post.getAuthorId().equals(authorId))
                .sorted((p1, p2) -> p2.getCreateTime().compareTo(p1.getCreateTime()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Post> getPostsWithFilter(Long categoryId, Integer status, Integer page, Integer pageSize) {
        // 获取当前登录用户
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth != null ? auth.getName() : null;
        User currentUser = username != null ? userRepository.findByUsername(username) : null;
        
        // 获取所有未删除的帖子，同时加载作者和分类信息
        List<Post> allPosts = postRepository.findAllWithAuthorAndCategory();
        
        List<Post> filteredPosts;
        
        // 如果是管理员（角色1或2），返回所有未封禁的帖子（status != 2）
        if (currentUser != null && (currentUser.getRole() == 1 || currentUser.getRole() == 2)) {
            final Long currentUserId = currentUser.getId();
            filteredPosts = allPosts.stream()
                    .filter(post -> !Integer.valueOf(2).equals(post.getStatus()) ||
                            (currentUserId != null && post.getAuthorId().equals(currentUserId)))
                    .toList();
        } else {
            // 普通用户或未登录用户，只能看到已审核通过的帖子或者自己的帖子
            final Long currentUserId = currentUser != null ? currentUser.getId() : null;
            filteredPosts = allPosts.stream()
                    .filter(post -> Integer.valueOf(0).equals(post.getStatus()) ||
                            (currentUserId != null && post.getAuthorId().equals(currentUserId)))
                    .toList();
        }
        
        // 如果有分类过滤，进一步筛选
        if (categoryId != null) {
            filteredPosts = filteredPosts.stream()
                    .filter(post -> Objects.equals(post.getCategoryId(), categoryId))
                    .toList();
        }
        
        // 如果有状态过滤，进一步筛选
        if (status != null) {
            filteredPosts = filteredPosts.stream()
                    .filter(post -> Objects.equals(post.getStatus(), status))
                    .toList();
        }
        
        // 按照置顶状态和创建时间排序，置顶帖子优先，同状态下按创建时间倒序
        filteredPosts = new java.util.ArrayList<>(filteredPosts);
        filteredPosts.sort((p1, p2) -> {
            if (!p1.getIsTop().equals(p2.getIsTop())) {
                return p2.getIsTop() - p1.getIsTop();
            }
            return p2.getCreateTime().compareTo(p1.getCreateTime());
        });
        
        // 验证分页参数
        if (page == null || page < 1) {
            page = 1;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 10;
        }
        
        // 分页处理
        int start = (page - 1) * pageSize;
        int end = Math.min(start + pageSize, filteredPosts.size());
        if (start >= end) {
            return List.of();
        }
        
        return filteredPosts.subList(start, end);
    }

    @Override
    public void increaseViewCount(Long postId) {
        Optional<Post> postOpt = postRepository.findById(postId);
        if (postOpt.isPresent()) {
            Post post = postOpt.get();
            post.setViewCount(post.getViewCount() + 1);
            postRepository.save(post);
        }
    }

    @Override
    public void increaseLikeCount(Long postId) {
        Optional<Post> postOpt = postRepository.findById(postId);
        if (postOpt.isPresent()) {
            Post post = postOpt.get();
            post.setLikeCount(post.getLikeCount() + 1);
            postRepository.save(post);
        }
    }

    @Override
    public void decreaseLikeCount(Long postId) {
        Optional<Post> postOpt = postRepository.findById(postId);
        if (postOpt.isPresent()) {
            Post post = postOpt.get();
            if (post.getLikeCount() > 0) {
                post.setLikeCount(post.getLikeCount() - 1);
                postRepository.save(post);
            }
        }
    }

    @Override
    public void increaseCommentCount(Long postId) {
        Optional<Post> postOpt = postRepository.findById(postId);
        if (postOpt.isPresent()) {
            Post post = postOpt.get();
            post.setCommentCount(post.getCommentCount() + 1);
            postRepository.save(post);
        }
    }

    @Override
    public void decreaseCommentCount(Long postId) {
        Optional<Post> postOpt = postRepository.findById(postId);
        if (postOpt.isPresent()) {
            Post post = postOpt.get();
            if (post.getCommentCount() > 0) {
                post.setCommentCount(post.getCommentCount() - 1);
                postRepository.save(post);
            }
        }
    }

    @Override
    public void increaseFavoriteCount(Long postId) {
        Optional<Post> postOpt = postRepository.findById(postId);
        if (postOpt.isPresent()) {
            Post post = postOpt.get();
            post.setFavoriteCount(post.getFavoriteCount() + 1);
            postRepository.save(post);
        }
    }

    @Override
    public void decreaseFavoriteCount(Long postId) {
        Optional<Post> postOpt = postRepository.findById(postId);
        if (postOpt.isPresent()) {
            Post post = postOpt.get();
            if (post.getFavoriteCount() > 0) {
                post.setFavoriteCount(post.getFavoriteCount() - 1);
                postRepository.save(post);
            }
        }
    }

    @Override
    public boolean lockPost(Long postId) {
        Optional<Post> postOpt = postRepository.findById(postId);
        if (postOpt.isPresent()) {
            Post post = postOpt.get();
            post.setIsLocked(1);
            post.setUpdateTime(LocalDateTime.now());
            postRepository.save(post);
            return true;
        }
        return false;
    }

    @Override
    public boolean unlockPost(Long postId) {
        Optional<Post> postOpt = postRepository.findById(postId);
        if (postOpt.isPresent()) {
            Post post = postOpt.get();
            post.setIsLocked(0);
            post.setUpdateTime(LocalDateTime.now());
            postRepository.save(post);
            return true;
        }
        return false;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Post> getPostsByStatus(Integer status) {
        // 获取所有未删除的帖子，同时加载作者和分类信息
        List<Post> allPosts = postRepository.findAllWithAuthorAndCategory();
        
        // 筛选出指定状态的帖子
        return allPosts.stream()
                .filter(post -> post.getStatus().equals(status))
                .sorted((p1, p2) -> p2.getCreateTime().compareTo(p1.getCreateTime()))
                .toList();
    }

    @Override
    public boolean updatePostStatus(Long postId, Integer status) {
        Optional<Post> postOpt = postRepository.findById(postId);
        if (postOpt.isPresent()) {
            Post post = postOpt.get();
            post.setStatus(status);
            post.setUpdateTime(LocalDateTime.now());
            // 如果是审核拒绝（状态2），清空拒绝原因（因为旧方法不支持设置原因）
            if (status == 2) {
                post.setRejectReason(null);
            }
            postRepository.save(post);
            return true;
        }
        return false;
    }

    @Override
    public boolean updatePostStatusWithReason(Long postId, Integer status, String rejectReason) {
        Optional<Post> postOpt = postRepository.findById(postId);
        if (postOpt.isPresent()) {
            Post post = postOpt.get();
            post.setStatus(status);
            post.setUpdateTime(LocalDateTime.now());
            // 如果是审核拒绝（状态2），设置拒绝原因
            if (status == 2) {
                post.setRejectReason(rejectReason);
            } else {
                // 其他状态清空拒绝原因
                post.setRejectReason(null);
            }
            postRepository.save(post);
            return true;
        }
        return false;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Post> getPostsLikedByUser(Long userId) {
        // 查询用户点赞的帖子记录
        List<LikeRecord> likeRecords = likeRecordRepository.findByUserIdAndTargetType(userId, 1);
        
        // 提取帖子ID列表
        List<Long> postIds = likeRecords.stream()
                .map(LikeRecord::getTargetId)
                .collect(Collectors.toList());
        
        // 查询并返回帖子列表，使用fetch join加载作者和分类信息
        List<Post> posts = postRepository.findByIdsWithAuthorAndCategory(postIds);
        
        // 按照创建时间倒序排序
        posts.sort((p1, p2) -> p2.getCreateTime().compareTo(p1.getCreateTime()));
        
        return posts;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Post> getPostsFavoritedByUser(Long userId) {
        // 查询用户收藏的帖子记录（从两个表查询）
        List<FavoritePost> favoritePosts = favoritePostRepository.findByUserId(userId);
        List<FavoriteRecord> favoriteRecords = favoriteRecordRepository.findByUserIdAndTargetType(userId, 1); // target_type=1 表示帖子
        
        // 提取帖子ID列表（从两个表合并）
        List<Long> postIds = new java.util.ArrayList<>();
        
        // 从旧的favorite_post表提取
        for (FavoritePost favoritePost : favoritePosts) {
            postIds.add(favoritePost.getPostId());
        }
        
        // 从新的favorite_record表提取
        for (FavoriteRecord favoriteRecord : favoriteRecords) {
            postIds.add(favoriteRecord.getTargetId());
        }
        
        // 去重
        postIds = postIds.stream().distinct().collect(Collectors.toList());
        
        // 如果没有收藏的帖子，返回空列表
        if (postIds.isEmpty()) {
            return new java.util.ArrayList<>();
        }
        
        // 查询并返回帖子列表，使用fetch join加载作者和分类信息
        List<Post> posts = postRepository.findByIdsWithAuthorAndCategory(postIds);
        
        // 按照创建时间倒序排序
        posts.sort((p1, p2) -> p2.getCreateTime().compareTo(p1.getCreateTime()));
        
        return posts;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Post> searchPosts(String keyword) {
        // 调用新的方法，不指定状态（null表示不筛选状态）
        return searchPostsWithStatus(keyword, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Post> searchPostsWithStatus(String keyword, Integer status) {
        // 获取当前登录用户
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth != null ? auth.getName() : null;
        User currentUser = username != null ? userRepository.findByUsername(username) : null;
        
        // 使用仓库方法搜索帖子（包含未删除的帖子）
        List<Post> allPosts = postRepository.searchByKeyword(keyword);
        
        List<Post> filteredPosts;
        
        // 如果是管理员（角色1或2），返回所有未封禁的搜索结果（status != 2）
        if (currentUser != null && (currentUser.getRole() == 1 || currentUser.getRole() == 2)) {
            final Long currentUserId = currentUser.getId();
            filteredPosts = allPosts.stream()
                    .filter(post -> !Integer.valueOf(2).equals(post.getStatus()) ||
                            (currentUserId != null && post.getAuthorId().equals(currentUserId)))
                    .collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));
        } else {
            // 普通用户或未登录用户，只能看到已审核通过的帖子或者自己的帖子
            final Long currentUserId = currentUser != null ? currentUser.getId() : null;
            filteredPosts = allPosts.stream()
                    .filter(post -> Integer.valueOf(0).equals(post.getStatus()) ||
                            (currentUserId != null && post.getAuthorId().equals(currentUserId)))
                    .collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));
        }
        
        // 如果有状态筛选，进一步过滤
        if (status != null) {
            filteredPosts = filteredPosts.stream()
                    .filter(post -> Objects.equals(post.getStatus(), status))
                    .collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));
        }
        
        // 按照置顶状态和创建时间排序，置顶帖子优先，同状态下按创建时间倒序
        filteredPosts.sort((p1, p2) -> {
            if (!p1.getIsTop().equals(p2.getIsTop())) {
                return p2.getIsTop() - p1.getIsTop();
            }
            return p2.getCreateTime().compareTo(p1.getCreateTime());
        });
        
        return filteredPosts;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Post> getDeletedPostsByAuthorId(Long authorId) {
        // 直接调用仓库方法查询已删除的帖子
        List<Post> deletedPosts = postRepository.findDeletedByAuthorId(authorId);
        // 按删除时间（或更新时间）倒序排序
        deletedPosts.sort((p1, p2) -> p2.getUpdateTime().compareTo(p1.getUpdateTime()));
        return deletedPosts;
    }

    @Override
    public long countPosts() {
        return postRepository.countByIsDeleted(0);
    }

    @Override
    @Transactional
    public boolean banPost(Long postId, String reason) {
        Optional<Post> postOpt = postRepository.findById(postId);
        if (postOpt.isPresent()) {
            Post post = postOpt.get();
            // 设置状态为封禁（2）
            post.setStatus(2);
            post.setUpdateTime(LocalDateTime.now());
            postRepository.save(post);
            
            // 发送消息通知给帖子作者
            Long authorId = post.getAuthorId();
            String content = "您的帖子《" + post.getTitle() + "》已被封禁。";
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
    public boolean unbanPost(Long postId) {
        Optional<Post> postOpt = postRepository.findById(postId);
        if (postOpt.isPresent()) {
            Post post = postOpt.get();
            // 设置状态为正常（0）
            post.setStatus(0);
            post.setUpdateTime(LocalDateTime.now());
            postRepository.save(post);
            
            // 发送消息通知给帖子作者
            Long authorId = post.getAuthorId();
            String content = "您的帖子《" + post.getTitle() + "》已解禁，现在可以正常显示。";
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
    public List<Post> getBannedPosts() {
        // 获取所有未删除的帖子，同时加载作者和分类信息
        List<Post> allPosts = postRepository.findAllWithAuthorAndCategory();
        
        // 筛选出封禁状态的帖子（status=2）
        return allPosts.stream()
                .filter(post -> post.getStatus() != null && post.getStatus() == 2)
                .sorted((p1, p2) -> p2.getUpdateTime().compareTo(p1.getUpdateTime()))
                .toList();
    }
    
    @Override
    @Transactional
    public boolean resubmitPost(Long postId) {
        Optional<Post> postOpt = postRepository.findById(postId);
        if (postOpt.isPresent()) {
            Post post = postOpt.get();
            // 检查当前状态是否为已拒绝（2）
            if (post.getStatus() != null && post.getStatus() == 2) {
                // 将状态改为审核中（1），清空拒绝原因
                post.setStatus(1);
                post.setRejectReason(null);
                post.setUpdateTime(LocalDateTime.now());
                postRepository.save(post);
                
                // 发送消息通知给帖子作者
                Long authorId = post.getAuthorId();
                String content = "您的帖子《" + post.getTitle() + "》已重新提交，等待审核。";
                Message message = new Message();
                message.setUserId(authorId);
                message.setContent(content);
                message.setCreateTime(LocalDateTime.now());
                message.setIsRead(0);
                messageService.createMessage(message);
                
                return true;
            } else {
                throw new RuntimeException("只有被拒绝的帖子才能重新提交");
            }
        }
        return false;
    }
}