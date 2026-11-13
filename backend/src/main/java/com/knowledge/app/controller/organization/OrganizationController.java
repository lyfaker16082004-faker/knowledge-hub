package com.knowledge.app.controller.organization;

import com.knowledge.app.repository.*;
import com.knowledge.app.common.response.ApiResponse;
import com.knowledge.app.common.exception.ApiException;
import java.util.List;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;

@RestController
@Transactional(readOnly = true)
public class OrganizationController {
    private final InstitutionRepository institutions;
    private final FacultyRepository faculties;

    public OrganizationController(InstitutionRepository institutions, FacultyRepository faculties) {
        this.institutions = institutions;
        this.faculties = faculties;
    }

    @GetMapping("/api/institutions")
    public ApiResponse<List<InstitutionInfo>> institutions() {
        return ApiResponse.ok(institutions.findAllByOrderByNameAsc().stream()
                .map(i -> new InstitutionInfo(i.getId(), i.getName(), i.getType())).toList());
    }

    @GetMapping("/api/institutions/{id}/faculties")
    public ApiResponse<List<FacultyInfo>> faculties(@PathVariable Long id) {
        var school = institutions.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy trường/tổ chức"));
        return ApiResponse.ok("OTHER".equals(school.getType()) ? List.of()
                : faculties.findByInstitutionIdOrderByNameAsc(id).stream()
                        .map(f -> new FacultyInfo(f.getId(), f.getInstitution().getId(), f.getName())).toList());
    }

    public record InstitutionInfo(Long id, String name, String type) {
    }

    public record FacultyInfo(Long id, Long institutionId, String name) {
    }
}
