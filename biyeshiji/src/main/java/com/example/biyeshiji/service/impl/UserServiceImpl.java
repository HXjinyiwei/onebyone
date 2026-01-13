package com.example.biyeshiji.service.impl;

import com.example.biyeshiji.entity.User;
import com.example.biyeshiji.exception.UserBannedException;
import com.example.biyeshiji.repository.UserRepository;
import com.example.biyeshiji.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;


    @Override
    public User login(String username, String password) {
        User user = userRepository.findByUsername(username);
        if (user == null) {
            return null;
        }
        // 如果密码为null，只查询用户信息，不验证密码
        if (password != null) {
            if (!passwordEncoder.matches(password, user.getUserPassword())) {
                return null;
            }
            // 检查封禁状态
            if (user.getStatus() == 1) {
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime banUntil = user.getBanUntil();
                // 如果banUntil不为空且已过期，自动解封
                if (banUntil != null && now.isAfter(banUntil)) {
                    user.setStatus(0);
                    user.setBanUntil(null);
                    userRepository.save(user);
                } else {
                    // 仍然封禁，抛出异常
                    throw new UserBannedException("用户已被封禁", banUntil);
                }
            }
        }
        return user;
    }

    @Override
    public User register(String username, String password, String nickname) {
        if (userRepository.findByUsername(username) != null) {
            return null; // 用户名已存在
        }
        User user = new User();
        user.setUsername(username);
        
        // 处理昵称，如果为空则自动生成
        if (nickname == null || nickname.trim().isEmpty()) {
            // 生成随机数，格式为用户xxxx
            int randomNum = (int) (Math.random() * 9000) + 1000;
            nickname = "用户" + randomNum;
        }
        user.setNickname(nickname);
        
        user.setUserPassword(passwordEncoder.encode(password));
        user.setStatus(0); // 默认正常状态
        user.setRole(0); // 默认普通用户
        return userRepository.save(user);
    }

    @Override
    public boolean updateNickname(Long userId, String nickname) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return false;
        }
        user.setNickname(nickname);
        userRepository.save(user);
        return true;
    }

    @Override
    public boolean updatePassword(Long userId, String oldPassword, String newPassword) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return false;
        }
        // 验证原密码是否正确
        if (!passwordEncoder.matches(oldPassword, user.getUserPassword())) {
            return false;
        }
        // 更新密码
        user.setUserPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        return true;
    }

    @Override
    public boolean resetPassword(Long userId, String newPassword) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return false;
        }
        // 更新密码
        user.setUserPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        return true;
    }

    @Override
    public long countUsers() {
        return userRepository.count();
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public List<User> searchUsers(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return userRepository.findAll();
        }
        return userRepository.searchUsers(keyword.trim());
    }

    @Override
    public boolean deleteUser(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return false;
        }
        // 逻辑删除：设置 is_deleted 标志（如果存在）或物理删除
        // 由于用户表没有 is_deleted 字段，我们使用物理删除
        userRepository.delete(user);
        return true;
    }

    @Override
    public boolean toggleBan(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return false;
        }
        // 切换状态：0 -> 1, 1 -> 0
        int newStatus = user.getStatus() == 0 ? 1 : 0;
        user.setStatus(newStatus);
        if (newStatus == 0) {
            // 解封时清空封禁截止时间
            user.setBanUntil(null);
        } else {
            // 封禁时如果不指定时长，则默认永久封禁（banUntil = null）
            // 注意：这里不设置banUntil，因为后续可能通过banUser方法设置
            // 保持原样
        }
        userRepository.save(user);
        return true;
    }

    @Override
    public boolean banUser(Long userId, String duration) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return false;
        }
        // 设置封禁状态为1
        user.setStatus(1);
        LocalDateTime banUntil = null;
        if (duration != null) {
            LocalDateTime now = LocalDateTime.now();
            switch (duration) {
                case "1h":
                    banUntil = now.plusHours(1);
                    break;
                case "1d":
                    banUntil = now.plusDays(1);
                    break;
                case "1w":
                case "7d":
                    banUntil = now.plusDays(7);
                    break;
                case "1M":
                case "30d":
                    banUntil = now.plusDays(30);
                    break;
                case "1y":
                case "365d":
                    banUntil = now.plusDays(365);
                    break;
                case "permanent":
                    // banUntil = null 表示永久封禁
                    break;
                case "0":
                    // 解封
                    user.setStatus(0);
                    user.setBanUntil(null);
                    userRepository.save(user);
                    return true;
                default:
                    // 默认永久封禁
                    break;
            }
        }
        user.setBanUntil(banUntil);
        userRepository.save(user);
        return true;
    }

    @Override
    public boolean promoteUser(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return false;
        }
        // 只能将普通用户（role=0）提升为管理员（role=1）
        if (user.getRole() == 0) {
            user.setRole(1);
            userRepository.save(user);
            return true;
        }
        return false;
    }

    @Override
    public boolean demoteUser(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return false;
        }
        // 只能将管理员（role=1）降级为普通用户（role=0）
        if (user.getRole() == 1) {
            user.setRole(0);
            userRepository.save(user);
            return true;
        }
        return false;
    }
}