package com.knowledge.app.service.organization;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.knowledge.app.common.exception.ApiException;
import com.knowledge.app.model.dto.response.user.OrganizationInfo;
import com.knowledge.app.model.entity.Faculty;
import com.knowledge.app.model.entity.Institution;
import com.knowledge.app.model.enums.InstitutionType;
import com.knowledge.app.repository.FacultyRepository;
import com.knowledge.app.repository.InstitutionRepository;

/**
 * Kiểm tra và tra cứu trường/khoa.
 */
@Service
@Transactional(readOnly = true)
public class OrganizationService {

    private final InstitutionRepository institutionRepository;
    private final FacultyRepository facultyRepository;

    public OrganizationService(InstitutionRepository institutionRepository, FacultyRepository facultyRepository) {
        this.institutionRepository = institutionRepository;
        this.facultyRepository = facultyRepository;
    }

    /** Kết quả: trường và khoa đã được kiểm tra (có thể null). */
    public record Selection(Institution institution, Faculty faculty) {
    }

    /**
     * Kiểm tra cặp (trường, khoa) người dùng chọn có hợp lệ không.
     *
     * Quy tắc:
     * - Không chọn trường thì cũng không được chọn khoa
     * - Trường phải tồn tại
     * - Tổ chức loại "OTHER" không có khoa
     * - Khoa phải thuộc đúng trường đã chọn
     */
    public Selection resolve(Long institutionId, Long facultyId) {
        if (institutionId == null) {
            if (facultyId != null) {
                throw invalid("Cần chọn trường/tổ chức trước khi chọn khoa");
            }
            return new Selection(null, null);
        }

        Institution institution = institutionRepository.findById(institutionId)
                .orElseThrow(() -> invalid("Trường/tổ chức không tồn tại"));

        if (InstitutionType.OTHER.is(institution.getType()) && facultyId != null) {
            throw invalid("Lựa chọn Khác không sử dụng khoa/chuyên ngành");
        }

        Faculty faculty = null;
        if (facultyId != null) {
            faculty = facultyRepository.findByIdAndInstitutionId(facultyId, institutionId)
                    .orElseThrow(() -> invalid("Khoa không thuộc trường/tổ chức đã chọn"));
        }

        return new Selection(institution, faculty);
    }

    /** Chuyển trường/khoa sang DTO. Không có trường thì trả về null. */
    public OrganizationInfo info(Institution institution, Faculty faculty) {
        if (institution == null) {
            return null;
        }

        Long facultyId = null;
        String facultyName = null;
        if (faculty != null) {
            facultyId = faculty.getId();
            facultyName = faculty.getName();
        }

        return new OrganizationInfo(institution.getId(), institution.getName(), institution.getType(),
                facultyId, facultyName);
    }

    private ApiException invalid(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message);
    }
}
