package com.example.biyeshiji.service;

import com.example.biyeshiji.entity.Message;

import java.util.List;

public interface MessageService {
    Message createMessage(Message message);
    List<Message> getMessagesByUserId(Long userId);
    List<Message> getMessagesByUserIdAndIsRead(Long userId, Integer isRead);
    int getUnreadMessageCount(Long userId);
    void markMessageAsRead(Long messageId);
    void markAllMessagesAsRead(Long userId);
    void deleteMessage(Long messageId);
    void deleteAllMessages(Long userId);
}