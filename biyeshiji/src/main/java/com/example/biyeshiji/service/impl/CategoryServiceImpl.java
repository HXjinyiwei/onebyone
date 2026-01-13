package com.example.biyeshiji.service.impl;

import com.example.biyeshiji.entity.Category;
import com.example.biyeshiji.repository.CategoryRepository;
import com.example.biyeshiji.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CategoryServiceImpl implements CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    @Override
    public Category createCategory(Category category) {
        return categoryRepository.save(category);
    }

    @Override
    public Category updateCategory(Category category) {
        Optional<Category> existingCategoryOpt = categoryRepository.findById(category.getId());
        if (existingCategoryOpt.isPresent()) {
            Category existingCategory = existingCategoryOpt.get();
            existingCategory.setName(category.getName());
            existingCategory.setDescription(category.getDescription());
            existingCategory.setParentId(category.getParentId());
            existingCategory.setSortOrder(category.getSortOrder());
            existingCategory.setIsActive(category.getIsActive());
            return categoryRepository.save(existingCategory);
        }
        return null;
    }

    @Override
    public boolean deleteCategory(Long categoryId) {
        Optional<Category> categoryOpt = categoryRepository.findById(categoryId);
        if (categoryOpt.isPresent()) {
            Category category = categoryOpt.get();
            category.setIsDeleted(1);
            return categoryRepository.save(category) != null;
        }
        return false;
    }

    @Override
    public Category getCategoryById(Long categoryId) {
        Optional<Category> categoryOpt = categoryRepository.findById(categoryId);
        return categoryOpt.orElse(null);
    }

    @Override
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    @Override
    public List<Category> getChildCategories(Long parentId) {
        // 这里需要使用JPA的查询方法，或者自定义查询
        // 目前先返回所有分类，后续可以扩展
        return categoryRepository.findAll();
    }
}