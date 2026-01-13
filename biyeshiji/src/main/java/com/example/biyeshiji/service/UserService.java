package com.example.biyeshiji.service;

import com.example.biyeshiji.entity.User;
import java.util.List;

public interface UserService {
    User login(String username, String password);
    User register(String username, String password, String nickname);
    boolean updateNickname(Long userId, String nickname);
    boolean updatePassword(Long userId, String oldPassword, String newPassword);
    boolean resetPassword(Long userId, String newPassword);
    long countUsers();
    List<User> getAllUsers();
    List<User> searchUsers(String keyword);
    boolean deleteUser(Long userId);
    boolean toggleBan(Long userId); // 旧方法，切换状态（无时长）
    boolean banUser(Long userId, String duration); // 新方法，支持时长（1d, 7d, 30d, permanent）
    boolean promoteUser(Long userId); // 提升为管理员（仅高级管理员可操作）
    boolean demoteUser(Long userId); // 降级为普通用户（仅高级管理员可操作）
}