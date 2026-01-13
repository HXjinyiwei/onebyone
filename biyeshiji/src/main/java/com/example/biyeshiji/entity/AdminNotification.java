package com.example.biyeshiji.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "admin_notification")
public class AdminNotification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer type; // 1=解封请求，2=举报，3=审核结果，4=系统通知

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "related_id")
    private Long relatedId;

    @Column(name = "related_type", length = 50)
    private String relatedType;

    @Column(name = "sender_id")
    private Long senderId;

    @Column(nullable = false, columnDefinition = "tinyint default 0")
    private Integer status = 0; // 0=未处理，1=已处理，2=已忽略

    @Column(name = "admin_read", nullable = false, columnDefinition = "tinyint default 0")
    private Integer adminRead = 0; // 0=未读，1=已读

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    @PrePersist
    public void prePersist() {
        createTime = LocalDateTime.now();
        updateTime = LocalDateTime.now();
        if (status == null) status = 0;
        if (adminRead == null) adminRead = 0;
    }

    @PreUpdate
    public void preUpdate() {
        updateTime = LocalDateTime.now();
    }
}