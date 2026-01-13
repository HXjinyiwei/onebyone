package com.example.biyeshiji.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "post")
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "text")
    private String content;

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Column(name = "category_id")
    private Long categoryId;

    @Column(nullable = false, columnDefinition = "tinyint default 0")
    private Integer status; // 0正常，1审核中，2封禁

    @Column(name = "view_count", columnDefinition = "int default 0")
    private Integer viewCount;

    @Column(name = "like_count", columnDefinition = "int default 0")
    private Integer likeCount;

    @Column(name = "comment_count", columnDefinition = "int default 0")
    private Integer commentCount;

    @Column(name = "favorite_count", columnDefinition = "int default 0")
    private Integer favoriteCount;

    @Column(name = "is_locked", columnDefinition = "tinyint default 0")
    private Integer isLocked; // 0正常，1锁定

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    @Column(name = "is_deleted", columnDefinition = "tinyint default 0")
    private Integer isDeleted; // 0正常，1软删除
    
    @Column(name = "is_top", columnDefinition = "tinyint default 0")
    private Integer isTop; // 0不置顶，1置顶

    // 关联作者信息
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "author_id", referencedColumnName = "id", insertable = false, updatable = false)
    @JsonIgnoreProperties({"password", "email", "phone", "posts", "comments", "hibernateLazyInitializer", "handler"})
    private User author;
    
    // 关联分类信息
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id", referencedColumnName = "id", insertable = false, updatable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Category category;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createTime = now;
        updateTime = now;
        if (status == null) {
            status = 1; // 1表示审核中
        }
        if (viewCount == null) {
            viewCount = 0;
        }
        if (likeCount == null) {
            likeCount = 0;
        }
        if (commentCount == null) {
            commentCount = 0;
        }
        if (favoriteCount == null) {
            favoriteCount = 0;
        }
        if (isLocked == null) {
            isLocked = 0;
        }
        if (isDeleted == null) {
            isDeleted = 0;
        }
        if (isTop == null) {
            isTop = 0;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updateTime = LocalDateTime.now();
    }
}