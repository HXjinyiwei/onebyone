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
@Table(name = "category")
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    private String description;

    @Column(name = "parent_id", columnDefinition = "bigint default 0")
    private Long parentId;

    @Column(name = "sort_order", columnDefinition = "int default 0")
    private Integer sortOrder;

    @Column(name = "is_active", columnDefinition = "tinyint default 1")
    private Integer isActive; // 0禁用，1启用

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    @Column(name = "is_deleted", columnDefinition = "tinyint default 0")
    private Integer isDeleted; // 0正常，1软删除

    // 多对多反向映射
    @ManyToMany(mappedBy = "categories")
    @JsonIgnoreProperties({"categories", "hibernateLazyInitializer", "handler"})
    @EqualsAndHashCode.Exclude
    private Set<Novel> novels = new HashSet<>();

    @PrePersist
    public void prePersist() {
        createTime = LocalDateTime.now();
        if (parentId == null) {
            parentId = 0L;
        }
        if (sortOrder == null) {
            sortOrder = 0;
        }
        if (isActive == null) {
            isActive = 1;
        }
        if (isDeleted == null) {
            isDeleted = 0;
        }
    }
}