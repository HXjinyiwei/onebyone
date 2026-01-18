package com.example.biyeshiji.repository;

import com.example.biyeshiji.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findByName(String name);
    
    // 按类型查询分类
    List<Category> findByType(Integer type);
    
    // 按类型和是否激活查询
    List<Category> findByTypeAndIsActive(Integer type, Integer isActive);
    
    // 按类型、是否激活和是否删除查询
    List<Category> findByTypeAndIsActiveAndIsDeleted(Integer type, Integer isActive, Integer isDeleted);
    
    // 按是否激活和是否删除查询（不区分类型）
    List<Category> findByIsActiveAndIsDeleted(Integer isActive, Integer isDeleted);
    
    // 搜索分类：按名称模糊搜索、类型和状态组合查询
    List<Category> findByNameContainingAndTypeAndIsActive(String name, Integer type, Integer isActive);
}
