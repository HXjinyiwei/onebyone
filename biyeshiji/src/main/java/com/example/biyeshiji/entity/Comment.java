package com.example.biyeshiji.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "comment")
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "text")
    private String content;

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(name = "like_count", columnDefinition = "int default 0")
    private Integer likeCount;

    @Column(nullable = false, columnDefinition = "tinyint default 0")
    private Integer status; // 0正常，1审核中，2封禁

    @Column(name = "floor_number", nullable = false)
    private Integer floorNumber;

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    @Column(name = "is_deleted", columnDefinition = "tinyint default 0")
    private Integer isDeleted; // 0正常，1软删除

    // 关联作者信息
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", referencedColumnName = "id", insertable = false, updatable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private User author;
    
    // 关联帖子信息
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", referencedColumnName = "id", insertable = false, updatable = false)
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler", "author", "category"})
    private Post post;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        createTime = now;
        updateTime = now;
        if (status == null) {
            status = 0;
        }
        if (likeCount == null) {
            likeCount = 0;
        }
        if (floorNumber == null) {
            floorNumber = 1;
        }
        if (isDeleted == null) {
            isDeleted = 0;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updateTime = LocalDateTime.now();
    }
}