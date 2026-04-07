package com.example.biyeshiji.service;

import com.example.biyeshiji.entity.Category;

import java.util.List;

public interface CategoryService {
    Category createCategory(Category category);
    Category updateCategory(Category category);
    boolean deleteCategory(Long categoryId);
    Category getCategoryById(Long categoryId);
    List<Category> getAllCategories();
    
    // 新增方法：按类型查询分类
    List<Category> getCategoriesByType(Integer type);
    
    // 新增方法：按类型和状态查询分类
    List<Category> getCategoriesByTypeAndStatus(Integer type, Boolean isActive);
    
    // 新增方法：获取所有有效的分类（未删除且激活）
    List<Category> getActiveCategories();
    
    // 新增方法：按类型获取所有有效的分类（未删除且激活）
    List<Category> getActiveCategoriesByType(Integer type);
    
    // 新增方法：搜索分类（支持名称模糊搜索、类型和状态筛选）
    List<Category> searchCategories(String name, Integer type, Integer isActive);
    
    // 新增方法：分页搜索分类
    com.example.biyeshiji.common.PaginationResponse<Category> searchCategoriesWithPagination(
            String name, Integer type, Integer isActive, Integer page, Integer pageSize);
}