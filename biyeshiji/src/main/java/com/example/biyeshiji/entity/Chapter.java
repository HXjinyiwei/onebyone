package com.example.biyeshiji.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "chapter")
public class Chapter {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "novel_id", nullable = false)
    private Long novelId;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "text")
    private String content;

    @Column(name = "sort_order", columnDefinition = "int default 0")
    private Integer sortOrder;

    @Column(name = "word_count", columnDefinition = "int default 0")
    private Integer wordCount;

    @Column(name = "is_free", columnDefinition = "tinyint default 1")
    private Integer isFree; // 1免费，0收费

    @Column(name = "view_count", columnDefinition = "int default 0")
    private Integer viewCount;

    @Column(name = "audit_status", columnDefinition = "tinyint default 0")
    private Integer auditStatus; // 0=待审核，1=审核通过，2=审核拒绝，3=封禁

    @Column(name = "reject_reason", length = 500)
    private String rejectReason; // 审核拒绝原因

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    // 关联小说信息
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "novel_id", referencedColumnName = "id", insertable = false, updatable = false)
    @JsonIgnoreProperties({"chapters", "hibernateLazyInitializer", "handler"})
    private Novel novel;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createTime = now;
        updateTime = now;
        if (sortOrder == null) {
            sortOrder = 0;
        }
        if (wordCount == null) {
            wordCount = 0;
        }
        if (isFree == null) {
            isFree = 1;
        }
        if (viewCount == null) {
            viewCount = 0;
        }
        if (auditStatus == null) {
            auditStatus = 0; // 默认待审核
        }
        // 自动计算字数（基于内容长度）
        if (content != null) {
            wordCount = content.length();
        }
    }

    @PreUpdate
    public void preUpdate() {
        updateTime = LocalDateTime.now();
        // 更新字数
        if (content != null) {
            wordCount = content.length();
        }
    }
}