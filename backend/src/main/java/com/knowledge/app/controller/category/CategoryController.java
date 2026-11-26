package com.knowledge.app.controller.category;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.knowledge.app.common.response.ApiResponse;
import com.knowledge.app.model.dto.response.category.CategoryOption;
import com.knowledge.app.service.category.CategoryService;

/**
 * API danh sách chủ đề đang hoạt động.
 */
@RestController
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping("/api/categories")
    public ApiResponse<List<CategoryOption>> categories() {
        return ApiResponse.ok(categoryService.activeCategories());
    }
}
