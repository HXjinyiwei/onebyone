package com.example.biyeshiji.controller;

import com.example.biyeshiji.common.PaginationResponse;
import com.example.biyeshiji.common.Response;
import com.example.biyeshiji.entity.User;
import com.example.biyeshiji.exception.UserBannedException;
import com.example.biyeshiji.repository.UserRepository;
import com.example.biyeshiji.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;
    
    @Autowired
    private UserRepository userRepository;

    @PostMapping("/login")
    public Response<User> login(@RequestParam String username, @RequestParam String password, HttpServletRequest request) {
        try {
            User user = userService.login(username, password);
            if (user == null) {
                return Response.error(401, "用户名或密码错误");
            }
            
            // 创建认证对象并设置到SecurityContext中
            List<GrantedAuthority> authorities = new ArrayList<>();
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
            
            // 如果是管理员或高级管理员，添加管理员权限
            if (user.getRole() == 1 || user.getRole() == 2) {
                authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
            }
            
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                user.getUsername(), null, authorities);
            
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            // 将认证信息存入Session
            HttpSession session = request.getSession(true);
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, SecurityContextHolder.getContext());
            
            return Response.success("登录成功", user);
        } catch (UserBannedException e) {
            LocalDateTime banUntil = e.getBanUntil();
            String message = "您的账号已被封禁";
            if (banUntil != null) {
                // 计算剩余时间
                Duration duration = Duration.between(LocalDateTime.now(), banUntil);
                long days = duration.toDays();
                long hours = duration.toHours() % 24;
                long minutes = duration.toMinutes() % 60;
                message += String.format("，剩余时长：%d天%d小时%d分钟", days, hours, minutes);
            } else {
                message += "，永久封禁";
            }
            return Response.error(403, message);
        }
    }

    @PostMapping("/register")
    public Response<User> register(@RequestParam String username, @RequestParam String password, @RequestParam(required = false) String nickname) {
        User user = userService.register(username, password, nickname);
        if (user == null) {
            // 检查是用户名已存在还是昵称已存在
            if (userRepository.findByUsername(username) != null) {
                return Response.error("用户名已存在");
            } else if (nickname != null && !nickname.trim().isEmpty() && userRepository.findByNickname(nickname.trim()) != null) {
                return Response.error("昵称已被使用");
            } else {
                return Response.error("注册失败");
            }
        }
        return Response.success("注册成功", user);
    }

    @GetMapping("/current")
    public Response<User> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return Response.error("未登录");
        }
        String username = auth.getName();
        System.out.println("当前认证用户名: " + username);
        System.out.println("认证类型: " + auth.getClass().getName());
        User user = userRepository.findByUsername(username);
        if (user == null) {
            return Response.error("用户不存在");
        }
        return Response.success("获取成功", user);
    }

    @PostMapping("/update-nickname")
    public Response<Void> updateNickname(@RequestParam String nickname) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            return Response.error("未登录");
        }
        String username = auth.getName();
        User user = userRepository.findByUsername(username);
        if (user == null) {
            return Response.error("用户不存在");
        }
        boolean result = userService.updateNickname(user.getId(), nickname);
        if (result) {
            return Response.success("昵称修改成功", null);
        } else {
            // 检查是否是昵称已被使用
            User existingUser = userRepository.findByNickname(nickname);
            if (existingUser != null && !existingUser.getId().equals(user.getId())) {
                return Response.error("昵称已被使用");
            } else {
                return Response.error("昵称修改失败");
            }
        }
    }

    @PostMapping("/update-password")
    public Response<Void> updatePassword(@RequestParam String oldPassword, @RequestParam String newPassword) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            return Response.error("未登录");
        }
        String username = auth.getName();
        User user = userRepository.findByUsername(username);
        if (user == null) {
            return Response.error("用户不存在");
        }
        boolean result = userService.updatePassword(user.getId(), oldPassword, newPassword);
        if (result) {
            return Response.success("密码修改成功", null);
        } else {
            return Response.error("原密码错误或修改失败");
        }
    }

    @GetMapping("/count")
    public Response<Long> countUsers() {
        long count = userService.countUsers();
        return Response.success("获取成功", count);
    }

    @GetMapping("/all")
    public Response<List<User>> getAllUsers(@RequestParam(required = false) String search) {
        // 检查当前用户是否为管理员或高级管理员
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return Response.error("未登录");
        }
        String username = auth.getName();
        User currentUser = userRepository.findByUsername(username);
        if (currentUser == null || (currentUser.getRole() != 1 && currentUser.getRole() != 2)) {
            return Response.error("权限不足，仅管理员或高级管理员可访问");
        }
        List<User> users;
        if (search != null && !search.trim().isEmpty()) {
            users = userService.searchUsers(search.trim());
        } else {
            users = userService.getAllUsers();
        }
        return Response.success("获取成功", users);
    }

    @PostMapping("/reset-password")
    public Response<Void> resetPassword(@RequestParam Long userId, @RequestParam(defaultValue = "123456") String newPassword) {
        // 检查当前用户是否为管理员或高级管理员
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return Response.error("未登录");
        }
        String username = auth.getName();
        User currentUser = userRepository.findByUsername(username);
        if (currentUser == null || (currentUser.getRole() != 1 && currentUser.getRole() != 2)) {
            return Response.error("权限不足，仅管理员或高级管理员可操作");
        }
        // 获取目标用户
        User targetUser = userRepository.findById(userId).orElse(null);
        if (targetUser == null) {
            return Response.error("用户不存在");
        }
        // 不能操作自己
        if (currentUser.getId().equals(targetUser.getId())) {
            return Response.error("不能操作自己");
        }
        // 权限分级：普通管理员不能操作高级管理员
        if (currentUser.getRole() == 1 && targetUser.getRole() == 2) {
            return Response.error("权限不足，普通管理员不能操作高级管理员");
        }
        // 同级管理员之间不能相互操作
        if (currentUser.getRole() == targetUser.getRole() && (currentUser.getRole() == 1 || currentUser.getRole() == 2)) {
            return Response.error("权限不足，同级管理员之间不能相互操作");
        }
        boolean result = userService.resetPassword(userId, newPassword);
        if (result) {
            return Response.success("密码重置成功", null);
        } else {
            return Response.error("密码重置失败，用户不存在");
        }
    }

    @PostMapping("/delete")
    public Response<Void> deleteUser(@RequestParam Long userId) {
        // 检查当前用户是否为管理员或高级管理员
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return Response.error("未登录");
        }
        String username = auth.getName();
        User currentUser = userRepository.findByUsername(username);
        if (currentUser == null || (currentUser.getRole() != 1 && currentUser.getRole() != 2)) {
            return Response.error("权限不足，仅管理员或高级管理员可操作");
        }
        // 获取目标用户
        User targetUser = userRepository.findById(userId).orElse(null);
        if (targetUser == null) {
            return Response.error("用户不存在");
        }
        // 不能操作自己
        if (currentUser.getId().equals(targetUser.getId())) {
            return Response.error("不能操作自己");
        }
        // 权限分级：普通管理员不能操作高级管理员
        if (currentUser.getRole() == 1 && targetUser.getRole() == 2) {
            return Response.error("权限不足，普通管理员不能操作高级管理员");
        }
        // 同级管理员之间不能相互操作
        if (currentUser.getRole() == targetUser.getRole() && (currentUser.getRole() == 1 || currentUser.getRole() == 2)) {
            return Response.error("权限不足，同级管理员之间不能相互操作");
        }
        boolean result = userService.deleteUser(userId);
        if (result) {
            return Response.success("用户删除成功", null);
        } else {
            return Response.error("用户删除失败，用户不存在");
        }
    }

    @PostMapping("/toggle-ban")
    public Response<Void> toggleBan(@RequestParam Long userId) {
        // 检查当前用户是否为管理员或高级管理员
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return Response.error("未登录");
        }
        String username = auth.getName();
        User currentUser = userRepository.findByUsername(username);
        if (currentUser == null || (currentUser.getRole() != 1 && currentUser.getRole() != 2)) {
            return Response.error("权限不足，仅管理员或高级管理员可操作");
        }
        // 获取目标用户
        User targetUser = userRepository.findById(userId).orElse(null);
        if (targetUser == null) {
            return Response.error("用户不存在");
        }
        // 不能操作自己
        if (currentUser.getId().equals(targetUser.getId())) {
            return Response.error("不能操作自己");
        }
        // 权限分级：普通管理员不能操作高级管理员
        if (currentUser.getRole() == 1 && targetUser.getRole() == 2) {
            return Response.error("权限不足，普通管理员不能操作高级管理员");
        }
        // 同级管理员之间不能相互操作
        if (currentUser.getRole() == targetUser.getRole() && (currentUser.getRole() == 1 || currentUser.getRole() == 2)) {
            return Response.error("权限不足，同级管理员之间不能相互操作");
        }
        boolean result = userService.toggleBan(userId);
        if (result) {
            return Response.success("用户状态切换成功", null);
        } else {
            return Response.error("用户状态切换失败，用户不存在");
        }
    }

    @PostMapping("/ban")
    public Response<Void> banUser(@RequestParam Long userId, @RequestParam String duration) {
        // 检查当前用户是否为管理员或高级管理员
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return Response.error("未登录");
        }
        String username = auth.getName();
        User currentUser = userRepository.findByUsername(username);
        if (currentUser == null || (currentUser.getRole() != 1 && currentUser.getRole() != 2)) {
            return Response.error("权限不足，仅管理员或高级管理员可操作");
        }
        // 获取目标用户
        User targetUser = userRepository.findById(userId).orElse(null);
        if (targetUser == null) {
            return Response.error("用户不存在");
        }
        // 不能操作自己
        if (currentUser.getId().equals(targetUser.getId())) {
            return Response.error("不能操作自己");
        }
        // 权限分级：普通管理员不能操作高级管理员
        if (currentUser.getRole() == 1 && targetUser.getRole() == 2) {
            return Response.error("权限不足，普通管理员不能操作高级管理员");
        }
        // 同级管理员之间不能相互操作
        if (currentUser.getRole() == targetUser.getRole() && (currentUser.getRole() == 1 || currentUser.getRole() == 2)) {
            return Response.error("权限不足，同级管理员之间不能相互操作");
        }
        boolean result = userService.banUser(userId, duration);
        if (result) {
            return Response.success("用户封禁成功", null);
        } else {
            return Response.error("用户封禁失败，用户不存在");
        }
    }

    @PostMapping("/promote")
    public Response<Void> promoteUser(@RequestParam Long userId) {
        // 检查当前用户是否为高级管理员（role=2）
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return Response.error("未登录");
        }
        String username = auth.getName();
        User currentUser = userRepository.findByUsername(username);
        if (currentUser == null || currentUser.getRole() != 2) {
            return Response.error("权限不足，仅高级管理员可操作");
        }
        boolean result = userService.promoteUser(userId);
        if (result) {
            return Response.success("用户已提升为管理员", null);
        } else {
            return Response.error("提升失败，用户不存在或已是管理员");
        }
    }

    @PostMapping("/demote")
    public Response<Void> demoteUser(@RequestParam Long userId) {
        // 检查当前用户是否为高级管理员（role=2）
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return Response.error("未登录");
        }
        String username = auth.getName();
        User currentUser = userRepository.findByUsername(username);
        if (currentUser == null || currentUser.getRole() != 2) {
            return Response.error("权限不足，仅高级管理员可操作");
        }
        boolean result = userService.demoteUser(userId);
        if (result) {
            return Response.success("管理员已降级为普通用户", null);
        } else {
            return Response.error("降级失败，用户不存在或不是管理员");
        }
    }

    @GetMapping("/check-nickname")
    public Response<Boolean> checkNickname(@RequestParam String nickname, @RequestParam(required = false) Long excludeUserId) {
        if (nickname == null || nickname.trim().isEmpty()) {
            return Response.success("昵称为空", false);
        }
        
        User existingUser = userRepository.findByNickname(nickname.trim());
        boolean isDuplicate = false;
        
        if (existingUser != null) {
            if (excludeUserId != null) {
                // 排除指定用户（用于修改昵称时检查）
                isDuplicate = !existingUser.getId().equals(excludeUserId);
            } else {
                // 注册时检查
                isDuplicate = true;
            }
        }
        
        return Response.success("检查完成", isDuplicate);
    }

    /**
     * 获取用户列表（分页版）
     * @param search 搜索关键词（可选）
     * @param page 页码（默认1）
     * @param pageSize 每页大小（默认10）
     * @return 分页用户列表
     */
    @GetMapping("/all/page")
    public Response<PaginationResponse<User>> getAllUsersWithPagination(
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize) {
        // 检查当前用户是否为管理员或高级管理员
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return Response.error("未登录");
        }
        String username = auth.getName();
        User currentUser = userRepository.findByUsername(username);
        if (currentUser == null || (currentUser.getRole() != 1 && currentUser.getRole() != 2)) {
            return Response.error("权限不足，仅管理员或高级管理员可访问");
        }
        
        // 获取总记录数
        long totalRecords;
        if (search != null && !search.trim().isEmpty()) {
            totalRecords = userRepository.countSearchUsers(search.trim());
        } else {
            totalRecords = userRepository.count();
        }
        
        // 计算总页数
        int totalPages = (int) Math.ceil((double) totalRecords / pageSize);
        if (totalPages == 0) totalPages = 1;
        
        // 确保页码在有效范围内
        if (page < 1) page = 1;
        if (page > totalPages) page = totalPages;
        
        // 计算偏移量
        int offset = (page - 1) * pageSize;
        
        // 获取分页数据
        List<User> users;
        if (search != null && !search.trim().isEmpty()) {
            users = userRepository.searchUsersWithPagination(search.trim(), offset, pageSize);
        } else {
            users = userRepository.findAllWithPagination(offset, pageSize);
        }
        
        // 构建分页响应
        PaginationResponse<User> paginationResponse = new PaginationResponse<>();
        paginationResponse.setData(users);
        paginationResponse.setCurrentPage(page);
        paginationResponse.setPageSize(pageSize);
        paginationResponse.setTotalRecords(totalRecords);
        paginationResponse.setTotalPages(totalPages);
        paginationResponse.setHasNext(page < totalPages);
        paginationResponse.setHasPrevious(page > 1);
        
        return Response.success("获取用户列表成功", paginationResponse);
    }
}