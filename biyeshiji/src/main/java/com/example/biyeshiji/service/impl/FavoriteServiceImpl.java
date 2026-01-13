package com.example.biyeshiji.service.impl;

import com.example.biyeshiji.entity.FavoritePost;
import com.example.biyeshiji.entity.FavoriteRecord;
import com.example.biyeshiji.entity.Message;
import com.example.biyeshiji.entity.Novel;
import com.example.biyeshiji.entity.Post;
import com.example.biyeshiji.repository.FavoritePostRepository;
import com.example.biyeshiji.repository.FavoriteRecordRepository;
import com.example.biyeshiji.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FavoriteServiceImpl implements FavoriteService {

    @Autowired
    private FavoritePostRepository favoritePostRepository;

    @Autowired
    private FavoriteRecordRepository favoriteRecordRepository;

    @Autowired
    private PostService postService;

    @Autowired
    private NovelService novelService;

    @Autowired
    private ChapterService chapterService;

    @Autowired
    private MessageService messageService;

    // 旧方法（仅帖子）保持兼容
    @Override
    public boolean favoritePost(Long userId, Long postId) {
        return favorite(userId, 1, postId);
    }

    @Override
    public boolean unfavoritePost(Long userId, Long postId) {
        return unfavorite(userId, 1, postId);
    }

    @Override
    public boolean isFavorited(Long userId, Long postId) {
        return isFavorited(userId, 1, postId);
    }

    // 新通用方法
    @Override
    @Transactional
    public boolean favorite(Long userId, Integer targetType, Long targetId) {
        // 检查是否已经收藏
        if (favoriteRecordRepository.existsByUserIdAndTargetTypeAndTargetId(userId, targetType, targetId)) {
            return false; // 已经收藏
        }

        // 创建收藏记录
        FavoriteRecord record = new FavoriteRecord();
        record.setUserId(userId);
        record.setTargetType(targetType);
        record.setTargetId(targetId);
        favoriteRecordRepository.save(record);

        // 增加对应目标的收藏数
        increaseFavoriteCount(targetType, targetId);

        // 发送消息通知给目标作者（如果需要）
        sendFavoriteNotification(userId, targetType, targetId);

        return true;
    }

    @Override
    @Transactional
    public boolean unfavorite(Long userId, Integer targetType, Long targetId) {
        // 检查是否已经收藏
        FavoriteRecord record = favoriteRecordRepository.findByUserIdAndTargetTypeAndTargetId(userId, targetType, targetId);
        if (record == null) {
            return false; // 没有收藏
        }

        // 删除收藏记录
        favoriteRecordRepository.delete(record);

        // 减少对应目标的收藏数
        decreaseFavoriteCount(targetType, targetId);

        return true;
    }

    @Override
    public boolean isFavorited(Long userId, Integer targetType, Long targetId) {
        return favoriteRecordRepository.existsByUserIdAndTargetTypeAndTargetId(userId, targetType, targetId);
    }

    private void increaseFavoriteCount(Integer targetType, Long targetId) {
        switch (targetType) {
            case 1: // 帖子
                postService.increaseFavoriteCount(targetId);
                break;
            case 3: // 小说
                novelService.increaseFavoriteCount(targetId);
                break;
            case 4: // 章节
                // 章节暂无收藏计数，可以忽略或增加小说收藏数？
                break;
            default:
                // 忽略
        }
    }

    private void decreaseFavoriteCount(Integer targetType, Long targetId) {
        switch (targetType) {
            case 1:
                postService.decreaseFavoriteCount(targetId);
                break;
            case 3:
                novelService.decreaseFavoriteCount(targetId);
                break;
            case 4:
                // 忽略
                break;
            default:
        }
    }

    private void sendFavoriteNotification(Long userId, Integer targetType, Long targetId) {
        Long authorId = null;
        String title = null;
        String typeName = "";

        switch (targetType) {
            case 1: // 帖子
                Post post = postService.getPostById(targetId);
                if (post != null) {
                    authorId = post.getAuthorId();
                    title = post.getTitle();
                    typeName = "帖子";
                }
                break;
            case 3: // 小说
                Novel novel = novelService.getNovelById(targetId);
                if (novel != null) {
                    authorId = novel.getAuthorId();
                    title = novel.getTitle();
                    typeName = "小说";
                }
                break;
            case 4: // 章节（可选）
                // 章节暂无通知，可以忽略或扩展
                break;
            default:
                return;
        }

        // 如果作者存在且不是收藏者本人，则发送通知
        if (authorId != null && !authorId.equals(userId)) {
            Message message = new Message();
            message.setUserId(authorId);
            message.setContent("您的" + typeName + "《" + title + "》被收藏了！");
            message.setIsRead(0);
            messageService.createMessage(message);
        }
    }
}