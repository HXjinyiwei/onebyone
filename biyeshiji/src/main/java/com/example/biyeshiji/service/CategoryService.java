package com.example.biyeshiji.service;

import com.example.biyeshiji.entity.Category;

import java.util.List;

public interface CategoryService {
    Category createCategory(Category category);
    Category updateCategory(Category category);
    boolean deleteCategory(Long categoryId);
    Category getCategoryById(Long categoryId);
    List<Category> getAllCategories();
    List<Category> getChildCategories(Long parentId);
}