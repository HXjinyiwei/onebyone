package com.example.biyeshiji.service.impl;

import com.example.biyeshiji.entity.LikeRecord;
import com.example.biyeshiji.entity.Message;
import com.example.biyeshiji.entity.Post;
import com.example.biyeshiji.entity.Comment;
import com.example.biyeshiji.entity.Novel;
import com.example.biyeshiji.entity.Chapter;
import com.example.biyeshiji.repository.LikeRecordRepository;
import com.example.biyeshiji.service.LikeService;
import com.example.biyeshiji.service.PostService;
import com.example.biyeshiji.service.CommentService;
import com.example.biyeshiji.service.NovelService;
import com.example.biyeshiji.service.ChapterService;
import com.example.biyeshiji.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LikeServiceImpl implements LikeService {

    @Autowired
    private LikeRecordRepository likeRecordRepository;

    @Autowired
    private PostService postService;

    @Autowired
    private CommentService commentService;

    @Autowired
    private NovelService novelService;

    @Autowired
    private ChapterService chapterService;
    
    @Autowired
    private MessageService messageService;

    @Override
    public boolean like(Long userId, Integer targetType, Long targetId) {
        // 检查是否已经点赞
        LikeRecord existingLike = likeRecordRepository.findByUserIdAndTargetTypeAndTargetId(userId, targetType, targetId);
        if (existingLike != null) {
            return false; // 已经点赞
        }

        // 创建点赞记录
        LikeRecord likeRecord = new LikeRecord();
        likeRecord.setUserId(userId);
        likeRecord.setTargetType(targetType);
        likeRecord.setTargetId(targetId);
        likeRecordRepository.save(likeRecord);

        // 更新点赞数量
        Long authorId = null;
        String messageContent = "";
        if (targetType == 1) {
            // 帖子点赞
            postService.increaseLikeCount(targetId);
            Post post = postService.getPostById(targetId);
            if (post != null) {
                authorId = post.getAuthorId();
                messageContent = "您的帖子《" + post.getTitle() + "》被点赞了！";
            }
        } else if (targetType == 2) {
            // 评论点赞
            commentService.increaseLikeCount(targetId);
            Comment comment = commentService.getCommentById(targetId);
            if (comment != null) {
                authorId = comment.getAuthorId();
                String commentPreview = comment.getContent().length() > 50 ?
                    comment.getContent().substring(0, 50) + "..." : comment.getContent();
                messageContent = "您的评论被点赞了：" + commentPreview;
            }
        } else if (targetType == 3) {
            // 小说点赞
            novelService.increaseLikeCount(targetId);
            Novel novel = novelService.getNovelById(targetId);
            if (novel != null) {
                authorId = novel.getAuthorId();
                messageContent = "您的小说《" + novel.getTitle() + "》被点赞了！";
            }
        } else if (targetType == 4) {
            // 章节点赞
            chapterService.increaseViewCount(targetId); // 章节没有单独的点赞计数，暂时用浏览量代替？或者需要扩展章节点赞功能
            // 暂时不发送消息
        }
        
        // 发送消息通知
        if (authorId != null && userId != authorId) {
            Message message = new Message();
            message.setUserId(authorId);
            message.setContent(messageContent);
            message.setIsRead(0);
            messageService.createMessage(message);
        }

        return true;
    }

    @Override
    public boolean unlike(Long userId, Integer targetType, Long targetId) {
        // 检查是否已经点赞
        LikeRecord existingLike = likeRecordRepository.findByUserIdAndTargetTypeAndTargetId(userId, targetType, targetId);
        if (existingLike == null) {
            return false; // 没有点赞
        }

        // 删除点赞记录
        likeRecordRepository.delete(existingLike);

        // 更新点赞数量
        if (targetType == 1) {
            // 帖子取消点赞
            postService.decreaseLikeCount(targetId);
        } else if (targetType == 2) {
            // 评论取消点赞
            commentService.decreaseLikeCount(targetId);
        } else if (targetType == 3) {
            // 小说取消点赞
            novelService.decreaseLikeCount(targetId);
        } else if (targetType == 4) {
            // 章节取消点赞（暂无对应方法，暂不处理）
        }

        return true;
    }

    @Override
    public boolean isLiked(Long userId, Integer targetType, Long targetId) {
        LikeRecord existingLike = likeRecordRepository.findByUserIdAndTargetTypeAndTargetId(userId, targetType, targetId);
        return existingLike != null;
    }
}