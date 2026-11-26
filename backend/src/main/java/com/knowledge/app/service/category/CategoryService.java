package com.knowledge.app.service.category;

import java.util.List;

import com.knowledge.app.model.dto.response.category.CategoryOption;

/**
 * Chức năng liên quan đến chủ đề.
 * Code xử lý nằm trong service/impl/category/CategoryServiceImpl.
 */
public interface CategoryService {

    /** Danh sách chủ đề đang hoạt động, sắp xếp theo tên. */
    List<CategoryOption> activeCategories();
}
