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
            return Response.error("分类创建失败");
        }
    }

    @PostMapping("/update")
    public Response<Category> updateCategory(@RequestBody Category category) {
        Category updatedCategory = categoryService.updateCategory(category);
        if (updatedCategory != null) {
            return Response.success("分类更新成功", updatedCategory);
        } else {
            return Response.error("分类更新失败");
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

    @GetMapping("/child/{parentId}")
    public Response<List<Category>> getChildCategories(@PathVariable Long parentId) {
        List<Category> categories = categoryService.getChildCategories(parentId);
        return Response.success("获取子分类成功", categories);
    }
}