package com.example.biyeshiji.service.impl;

import com.example.biyeshiji.entity.Message;
import com.example.biyeshiji.repository.MessageRepository;
import com.example.biyeshiji.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MessageServiceImpl implements MessageService {

    @Autowired
    private MessageRepository messageRepository;

    @Override
    public Message createMessage(Message message) {
        return messageRepository.save(message);
    }

    @Override
    public List<Message> getMessagesByUserId(Long userId) {
        return messageRepository.findByUserIdOrderByCreateTimeDesc(userId);
    }

    @Override
    public List<Message> getMessagesByUserIdAndIsRead(Long userId, Integer isRead) {
        return messageRepository.findByUserIdAndIsReadOrderByCreateTimeDesc(userId, isRead);
    }

    @Override
    public int getUnreadMessageCount(Long userId) {
        return messageRepository.countByUserIdAndIsRead(userId, 0);
    }

    @Override
    public void markMessageAsRead(Long messageId) {
        Optional<Message> messageOpt = messageRepository.findById(messageId);
        if (messageOpt.isPresent()) {
            Message message = messageOpt.get();
            message.setIsRead(1);
            messageRepository.save(message);
        }
    }

    @Override
    public void markAllMessagesAsRead(Long userId) {
        List<Message> messages = messageRepository.findByUserIdOrderByCreateTimeDesc(userId);
        messages.forEach(message -> {
            message.setIsRead(1);
        });
        messageRepository.saveAll(messages);
    }

    @Override
    public void deleteMessage(Long messageId) {
        messageRepository.deleteById(messageId);
    }

    @Override
    public void deleteAllMessages(Long userId) {
        messageRepository.deleteByUserId(userId);
    }
}