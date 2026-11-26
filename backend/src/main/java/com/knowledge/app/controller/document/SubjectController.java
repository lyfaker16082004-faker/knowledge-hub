package com.knowledge.app.controller.document;

import java.util.ArrayList;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.knowledge.app.common.response.ApiResponse;
import com.knowledge.app.model.entity.Subject;
import com.knowledge.app.repository.SubjectRepository;

/**
 * API danh sách môn học.
 */
@RestController
public class SubjectController {

    private final SubjectRepository subjectRepository;

    public SubjectController(SubjectRepository subjectRepository) {
        this.subjectRepository = subjectRepository;
    }

    public record SubjectInfo(Long id, String name) {
    }

    /** Danh sách môn học, sắp xếp theo tên. */
    @GetMapping("/api/subjects")
    public ApiResponse<List<SubjectInfo>> list() {
        List<SubjectInfo> result = new ArrayList<>();
        for (Subject subject : subjectRepository.findAllByOrderByNameAsc()) {
            result.add(new SubjectInfo(subject.getId(), subject.getName()));
        }
        return ApiResponse.ok(result);
    }
}
