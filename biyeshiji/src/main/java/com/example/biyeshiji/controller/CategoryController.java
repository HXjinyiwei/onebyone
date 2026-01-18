package com.example.biyeshiji.controller;

import com.example.biyeshiji.common.Response;
import com.example.biyeshiji.entity.Category;
import com.example.biyeshiji.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/category")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    @PostMapping("/create")
    public Response<Category> createCategory(@RequestBody Category category) {
        Category createdCategory = categoryService.createCategory(category);
        if (createdCategory != null) {
            return Response.success("分类创建成功", createdCategory);
        } else {
            return Response.error("分类名称已存在");
        }
    }

    @PostMapping("/update")
    public Response<Category> updateCategory(@RequestBody Category category) {
        Category updatedCategory = categoryService.updateCategory(category);
        if (updatedCategory != null) {
            return Response.success("分类更新成功", updatedCategory);
        } else {
            return Response.error("分类名称已存在或分类不存在");
        }
    }

    @PostMapping("/delete/{id}")
    public Response<Void> deleteCategory(@PathVariable Long id) {
        boolean result = categoryService.deleteCategory(id);
        if (result) {
            return Response.success("分类删除成功", null);
        } else {
            return Response.error("分类删除失败");
        }
    }

    @GetMapping("/detail/{id}")
    public Response<Category> getCategoryById(@PathVariable Long id) {
        Category category = categoryService.getCategoryById(id);
        if (category != null) {
            return Response.success("获取分类成功", category);
        } else {
            return Response.error("分类不存在");
        }
    }

    @GetMapping("/list")
    public Response<List<Category>> getAllCategories() {
        List<Category> categories = categoryService.getAllCategories();
        return Response.success("获取分类列表成功", categories);
    }

    @GetMapping("/type/{type}")
    public Response<List<Category>> getCategoriesByType(@PathVariable Integer type) {
        List<Category> categories = categoryService.getCategoriesByType(type);
        return Response.success("获取分类成功", categories);
    }

    @GetMapping("/type/{type}/status/{status}")
    public Response<List<Category>> getCategoriesByTypeAndStatus(
            @PathVariable Integer type,
            @PathVariable Boolean status) {
        List<Category> categories = categoryService.getCategoriesByTypeAndStatus(type, status);
        return Response.success("获取分类成功", categories);
    }

    @GetMapping("/active")
    public Response<List<Category>> getActiveCategories() {
        List<Category> categories = categoryService.getActiveCategories();
        return Response.success("获取有效分类成功", categories);
    }

    @GetMapping("/active/type/{type}")
    public Response<List<Category>> getActiveCategoriesByType(@PathVariable Integer type) {
        List<Category> categories = categoryService.getActiveCategoriesByType(type);
        return Response.success("获取有效分类成功", categories);
    }
    
    @GetMapping("/search")
    public Response<List<Category>> searchCategories(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer type,
            @RequestParam(required = false) Integer isActive) {
        List<Category> categories = categoryService.searchCategories(name, type, isActive);
        return Response.success("搜索分类成功", categories);
    }
}