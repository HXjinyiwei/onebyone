package com.example.biyeshiji.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "user")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String username; // 用户账号

    @Column(name = "nickname", unique = true, length = 50)
    private String nickname; // 用户名字

    @JsonIgnore  // 序列化时忽略密码字段
    @Column(name = "userpassword", nullable = false, length = 100)
    private String userPassword;

    @Column(nullable = false, columnDefinition = "tinyint default 0")
    private Integer status; // 0正常，1封禁

    @Column(nullable = false, columnDefinition = "tinyint default 0")
    private Integer role; // 0用户，1管理员，2高级管理员

    @Column(name = "ban_until")
    private LocalDateTime banUntil; // 封禁截止时间，null表示永久封禁或未封禁

    // 判断用户是否被封禁（考虑banUntil）
    public boolean isBanned() {
        if (status == 1) {
            // 如果banUntil不为空且已过期，则自动解封
            if (banUntil != null && LocalDateTime.now().isAfter(banUntil)) {
                status = 0;
                banUntil = null;
                return false;
            }
            return true;
        }
        return false;
    }
}