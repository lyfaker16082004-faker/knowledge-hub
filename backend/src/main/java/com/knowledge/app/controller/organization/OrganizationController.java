package com.knowledge.app.controller.organization;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.knowledge.app.common.exception.ApiException;
import com.knowledge.app.common.response.ApiResponse;
import com.knowledge.app.model.entity.Faculty;
import com.knowledge.app.model.entity.Institution;
import com.knowledge.app.model.enums.InstitutionType;
import com.knowledge.app.repository.FacultyRepository;
import com.knowledge.app.repository.InstitutionRepository;

/**
 * API công khai: danh sách trường và khoa (dùng cho các ô chọn ở form đăng ký, hồ sơ...).
 */
@RestController
@Transactional(readOnly = true)
public class OrganizationController {

    private final InstitutionRepository institutionRepository;
    private final FacultyRepository facultyRepository;

    public OrganizationController(InstitutionRepository institutionRepository, FacultyRepository facultyRepository) {
        this.institutionRepository = institutionRepository;
        this.facultyRepository = facultyRepository;
    }

    public record InstitutionInfo(Long id, String name, String type) {
    }

    public record FacultyInfo(Long id, Long institutionId, String name) {
    }

    /** Danh sách trường / tổ chức. */
    @GetMapping("/api/institutions")
    public ApiResponse<List<InstitutionInfo>> institutions() {
        List<InstitutionInfo> result = new ArrayList<>();
        for (Institution institution : institutionRepository.findAllByOrderByNameAsc()) {
            result.add(new InstitutionInfo(institution.getId(), institution.getName(), institution.getType()));
        }
        return ApiResponse.ok(result);
    }

    /** Danh sách khoa của một trường. Tổ chức loại "OTHER" không có khoa. */
    @GetMapping("/api/institutions/{id}/faculties")
    public ApiResponse<List<FacultyInfo>> faculties(@PathVariable Long id) {
        Institution institution = institutionRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Không tìm thấy trường/tổ chức"));

        List<FacultyInfo> result = new ArrayList<>();
        if (InstitutionType.OTHER.is(institution.getType())) {
            return ApiResponse.ok(result);
        }

        for (Faculty faculty : facultyRepository.findByInstitutionIdOrderByNameAsc(id)) {
            result.add(new FacultyInfo(faculty.getId(), faculty.getInstitution().getId(), faculty.getName()));
        }
        return ApiResponse.ok(result);
    }
}
