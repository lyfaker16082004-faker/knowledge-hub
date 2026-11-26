package com.knowledge.app.service.impl.category;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.knowledge.app.model.dto.response.category.CategoryOption;
import com.knowledge.app.model.entity.Category;
import com.knowledge.app.repository.CategoryRepository;
import com.knowledge.app.service.category.CategoryService;

/**
 * Lấy danh sách chủ đề đang hoạt động.
 */
@Service
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryServiceImpl(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public List<CategoryOption> activeCategories() {
        List<CategoryOption> result = new ArrayList<>();
        for (Category category : categoryRepository.findByActiveTrueOrderByNameAsc()) {
            result.add(new CategoryOption(category.getId(), category.getName()));
        }
        return result;
    }
}
