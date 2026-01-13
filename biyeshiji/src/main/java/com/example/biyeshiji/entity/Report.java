package com.example.biyeshiji.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "report")
public class Report {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reporter_id", nullable = false)
    private Long reporterId;

    @Column(name = "target_type", nullable = false, length = 50)
    private String targetType; // post, novel, chapter, comment, user

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(length = 500)
    private String evidence; // 证据图片链接

    @Column(nullable = false, columnDefinition = "tinyint default 0")
    private Integer status = 0; // 0=待处理，1=已处理，2=已忽略

    @Column(name = "admin_id")
    private Long adminId;

    @Column(name = "handle_time")
    private LocalDateTime handleTime;

    @Column(name = "handle_result", columnDefinition = "TEXT")
    private String handleResult;

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    @PrePersist
    public void prePersist() {
        createTime = LocalDateTime.now();
        if (status == null) status = 0;
    }
}