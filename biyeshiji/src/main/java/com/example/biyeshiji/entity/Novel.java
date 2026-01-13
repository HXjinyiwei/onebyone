package com.example.biyeshiji.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Data
@Entity
@Table(name = "novel")
public class Novel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "cover_image", length = 500)
    private String coverImage;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Column(name = "pen_name", length = 100)
    private String penName;

    @Column(columnDefinition = "tinyint default 0")
    private Integer status; // 0=连载中，1=已完结，2=暂停

    @Column(name = "audit_status", columnDefinition = "tinyint default 0")
    private Integer auditStatus; // 0=待审核，1=审核通过，2=审核拒绝，3=封禁

    @Column(name = "view_count", columnDefinition = "int default 0")
    private Integer viewCount;

    @Column(name = "like_count", columnDefinition = "int default 0")
    private Integer likeCount;

    @Column(name = "favorite_count", columnDefinition = "int default 0")
    private Integer favoriteCount;

    @Column(name = "chapter_count", columnDefinition = "int default 0")
    private Integer chapterCount;

    @Column(name = "is_deleted", columnDefinition = "tinyint default 0")
    private Integer isDeleted; // 0正常，1软删除

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    // 关联作者信息
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "author_id", referencedColumnName = "id", insertable = false, updatable = false)
    @JsonIgnoreProperties({"password", "email", "phone", "posts", "comments", "hibernateLazyInitializer", "handler"})
    private User author;

    // 多对多关联分类
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "novel_category",
        joinColumns = @JoinColumn(name = "novel_id"),
        inverseJoinColumns = @JoinColumn(name = "category_id")
    )
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @EqualsAndHashCode.Exclude
    private Set<Category> categories = new HashSet<>();

    // 临时字段，用于接收前端传递的分类ID列表
    @Transient
    private Set<Long> categoryIds = new HashSet<>();

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createTime = now;
        updateTime = now;
        if (status == null) {
            status = 0; // 默认连载中
        }
        if (auditStatus == null) {
            auditStatus = 0; // 默认待审核
        }
        if (viewCount == null) {
            viewCount = 0;
        }
        if (likeCount == null) {
            likeCount = 0;
        }
        if (favoriteCount == null) {
            favoriteCount = 0;
        }
        if (chapterCount == null) {
            chapterCount = 0;
        }
        if (isDeleted == null) {
            isDeleted = 0;
        }
        if (penName == null || penName.trim().isEmpty()) {
            // 笔名默认为用户昵称，但需要在服务层设置，这里无法获取User
            // 留空，由服务层处理
        }
    }

    @PreUpdate
    public void preUpdate() {
        updateTime = LocalDateTime.now();
    }

    @PostLoad
    public void postLoad() {
        if (categories != null) {
            categoryIds = categories.stream().map(Category::getId).collect(java.util.stream.Collectors.toSet());
        }
    }
}
