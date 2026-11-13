package com.knowledge.app.controller.document;

import com.knowledge.app.repository.SubjectRepository;
import com.knowledge.app.common.response.ApiResponse;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
public class SubjectController {
    private final SubjectRepository subjects;

    public SubjectController(SubjectRepository subjects) {
        this.subjects = subjects;
    }

    @GetMapping("/api/subjects")
    public ApiResponse<List<SubjectInfo>> list() {
        return ApiResponse.ok(
                subjects.findAllByOrderByNameAsc().stream().map(s -> new SubjectInfo(s.getId(), s.getName())).toList());
    }

    public record SubjectInfo(Long id, String name) {
    }
}
