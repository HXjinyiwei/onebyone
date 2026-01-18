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
        // 检查分类名称是否已存在
        Optional<Category> existingCategory = categoryRepository.findByName(category.getName());
        if (existingCategory.isPresent()) {
            // 分类名称已存在，返回null表示创建失败
            return null;
        }
        return categoryRepository.save(category);
    }

    @Override
    public Category updateCategory(Category category) {
        Optional<Category> existingCategoryOpt = categoryRepository.findById(category.getId());
        if (existingCategoryOpt.isPresent()) {
            Category existingCategory = existingCategoryOpt.get();
            
            // 检查分类名称是否已存在（排除自身）
            Optional<Category> duplicateCategory = categoryRepository.findByName(category.getName());
            if (duplicateCategory.isPresent() && !duplicateCategory.get().getId().equals(category.getId())) {
                // 分类名称已存在且不是自身，返回null表示更新失败
                return null;
            }
            
            existingCategory.setName(category.getName());
            existingCategory.setDescription(category.getDescription());
            existingCategory.setIsActive(category.getIsActive());
            existingCategory.setType(category.getType());
            return categoryRepository.save(existingCategory);
        }
        return null;
    }

    @Override
    public boolean deleteCategory(Long categoryId) {
        // 检查分类是否存在
        if (categoryRepository.existsById(categoryId)) {
            // 物理删除，直接从数据库中删除
            categoryRepository.deleteById(categoryId);
            return true;
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
    public List<Category> getCategoriesByType(Integer type) {
        return categoryRepository.findByType(type);
    }

    @Override
    public List<Category> getCategoriesByTypeAndStatus(Integer type, Boolean isActive) {
        // 将Boolean转换为Integer：true -> 1, false -> 0
        Integer isActiveInt = isActive ? 1 : 0;
        return categoryRepository.findByTypeAndIsActive(type, isActiveInt);
    }

    @Override
    public List<Category> getActiveCategories() {
        return categoryRepository.findByIsActiveAndIsDeleted(1, 0);
    }

    @Override
    public List<Category> getActiveCategoriesByType(Integer type) {
        return categoryRepository.findByTypeAndIsActiveAndIsDeleted(type, 1, 0);
    }
    
    @Override
    public List<Category> searchCategories(String name, Integer type, Integer isActive) {
        // 如果所有参数都为null或空，返回所有分类
        if ((name == null || name.trim().isEmpty()) && type == null && isActive == null) {
            return categoryRepository.findAll();
        }
        
        // 由于JPA查询方法需要所有参数都不为null，我们需要根据参数情况选择不同的查询方法
        // 这里简化处理：先获取所有分类，然后在内存中过滤
        List<Category> allCategories = categoryRepository.findAll();
        
        // 过滤逻辑
        return allCategories.stream()
                .filter(category -> {
                    // 名称过滤
                    if (name != null && !name.trim().isEmpty()) {
                        String searchName = name.trim().toLowerCase();
                        String categoryName = category.getName() != null ? category.getName().toLowerCase() : "";
                        if (!categoryName.contains(searchName)) {
                            return false;
                        }
                    }
                    
                    // 类型过滤
                    if (type != null) {
                        if (!type.equals(category.getType())) {
                            return false;
                        }
                    }
                    
                    // 状态过滤
                    if (isActive != null) {
                        if (!isActive.equals(category.getIsActive())) {
                            return false;
                        }
                    }
                    
                    return true;
                })
                .toList();
    }
}