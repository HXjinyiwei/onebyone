package com.example.biyeshiji.repository;

import com.example.biyeshiji.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    User findByUsername(String username);
    
    User findByNickname(String nickname);
    
    @Query("SELECT u FROM User u WHERE " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.nickname) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "(u.role = 0 AND LOWER(:keyword) IN ('普通用户', '用户', '普通', 'user')) OR " +
           "(u.role = 1 AND LOWER(:keyword) IN ('管理员', 'admin', '管理')) OR " +
           "(u.role = 2 AND LOWER(:keyword) IN ('高级管理员', 'superadmin', '高级', '超级')) OR " +
           "(u.status = 0 AND LOWER(:keyword) IN ('正常', '正常状态', 'active', '启用')) OR " +
           "(u.status = 1 AND LOWER(:keyword) IN ('封禁', '封禁状态', 'banned', '禁用', '冻结'))")
    List<User> searchUsers(@Param("keyword") String keyword);
}