package com.knowledge.app.controller.category;
import com.knowledge.app.repository.CategoryRepository;
import com.knowledge.app.common.response.ApiResponse;
import java.util.List;
import org.springframework.web.bind.annotation.*;
@RestController
public class CategoryController {
 private final CategoryRepository categories;
 public CategoryController(CategoryRepository categories){this.categories=categories;}
 @GetMapping("/api/categories") public ApiResponse<List<Option>> categories(){return ApiResponse.ok(categories.findByActiveTrueOrderByNameAsc().stream().map(c->new Option(c.getId(),c.getName())).toList());}
 public record Option(Long id,String name){}
}
