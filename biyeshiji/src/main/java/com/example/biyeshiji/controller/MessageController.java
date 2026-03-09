package com.example.biyeshiji.controller;

import com.example.biyeshiji.common.Response;
import com.example.biyeshiji.entity.Message;
import com.example.biyeshiji.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/message")
public class MessageController {

    @Autowired
    private MessageService messageService;

    @GetMapping("/user/{userId}")
    public Response<List<Message>> getMessagesByUserId(@PathVariable Long userId) {
        List<Message> messages = messageService.getMessagesByUserId(userId);
        return Response.success("获取消息列表成功", messages);
    }

    @GetMapping("/user/{userId}/page")
    public Response<Page<Message>> getMessagesByUserIdWithPagination(@PathVariable Long userId,
                                                                     @RequestParam(defaultValue = "0") int page,
                                                                     @RequestParam(defaultValue = "10") int size) {
        Page<Message> messages = messageService.getMessagesByUserIdWithPagination(userId, page, size);
        return Response.success("获取消息列表成功", messages);
    }

    @GetMapping("/user/{userId}/filter")
    public Response<List<Message>> getMessagesByUserIdAndIsRead(@PathVariable Long userId,
                                                                @RequestParam(required = false) Integer isRead) {
        List<Message> messages;
        if (isRead == null) {
            messages = messageService.getMessagesByUserId(userId);
        } else {
            messages = messageService.getMessagesByUserIdAndIsRead(userId, isRead);
        }
        return Response.success("获取消息列表成功", messages);
    }

    @GetMapping("/user/{userId}/filter/page")
    public Response<Page<Message>> getMessagesByUserIdAndIsReadWithPagination(@PathVariable Long userId,
                                                                              @RequestParam(required = false) Integer isRead,
                                                                              @RequestParam(defaultValue = "0") int page,
                                                                              @RequestParam(defaultValue = "10") int size) {
        Page<Message> messages;
        if (isRead == null) {
            messages = messageService.getMessagesByUserIdWithPagination(userId, page, size);
        } else {
            messages = messageService.getMessagesByUserIdAndIsReadWithPagination(userId, isRead, page, size);
        }
        return Response.success("获取消息列表成功", messages);
    }

    @GetMapping("/unread-count/{userId}")
    public Response<Integer> getUnreadMessageCount(@PathVariable Long userId) {
        int count = messageService.getUnreadMessageCount(userId);
        return Response.success("获取未读消息数成功", count);
    }

    @PostMapping("/read/{messageId}")
    public Response<Void> markMessageAsRead(@PathVariable Long messageId) {
        messageService.markMessageAsRead(messageId);
        return Response.success("标记消息为已读成功", null);
    }

    @PostMapping("/read-all/{userId}")
    public Response<Void> markAllMessagesAsRead(@PathVariable Long userId) {
        messageService.markAllMessagesAsRead(userId);
        return Response.success("标记所有消息为已读成功", null);
    }

    @PostMapping("/delete/{messageId}")
    public Response<Void> deleteMessage(@PathVariable Long messageId) {
        messageService.deleteMessage(messageId);
        return Response.success("删除消息成功", null);
    }

    @PostMapping("/delete-all/{userId}")
    public Response<Void> deleteAllMessages(@PathVariable Long userId) {
        messageService.deleteAllMessages(userId);
        return Response.success("删除所有消息成功", null);
    }
}