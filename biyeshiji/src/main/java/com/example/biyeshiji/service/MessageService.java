package com.example.biyeshiji.service;

import com.example.biyeshiji.entity.Message;
import org.springframework.data.domain.Page;

import java.util.List;

public interface MessageService {
    Message createMessage(Message message);
    List<Message> getMessagesByUserId(Long userId);
    Page<Message> getMessagesByUserIdWithPagination(Long userId, int page, int size);
    List<Message> getMessagesByUserIdAndIsRead(Long userId, Integer isRead);
    Page<Message> getMessagesByUserIdAndIsReadWithPagination(Long userId, Integer isRead, int page, int size);
    int getUnreadMessageCount(Long userId);
    void markMessageAsRead(Long messageId);
    void markAllMessagesAsRead(Long userId);
    void deleteMessage(Long messageId);
    void deleteAllMessages(Long userId);
}