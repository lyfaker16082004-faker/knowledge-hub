package com.knowledge.app.service.organization;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.knowledge.app.common.exception.ApiException;
import com.knowledge.app.model.entity.Faculty;
import com.knowledge.app.model.entity.Institution;
import com.knowledge.app.model.enums.InstitutionType;
import com.knowledge.app.repository.DocumentRepository;
import com.knowledge.app.repository.FacultyRepository;
import com.knowledge.app.repository.InstitutionRepository;
import com.knowledge.app.repository.PostRepository;
import com.knowledge.app.repository.UserProfileRepository;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Admin quản lý danh sách Trường/Tổ chức và Khoa.
 *
 * Quy tắc quan trọng:
 * - Tên trường không được trùng; tên khoa không được trùng trong cùng một trường.
 * - Tổ chức loại "OTHER" (Khác) không có khoa, và phải luôn còn ít nhất 1 tổ chức loại này.
 * - Không được xoá trường/khoa đang được hồ sơ, tài liệu hoặc bài viết sử dụng.
 */
@Service
@Transactional
public class OrganizationAdminService {

    private final InstitutionRepository institutionRepository;
    private final FacultyRepository facultyRepository;
    private final UserProfileRepository userProfileRepository;
    private final DocumentRepository documentRepository;
    private final PostRepository postRepository;

    public OrganizationAdminService(InstitutionRepository institutionRepository,
            FacultyRepository facultyRepository,
            UserProfileRepository userProfileRepository,
            DocumentRepository documentRepository,
            PostRepository postRepository) {
        this.institutionRepository = institutionRepository;
        this.facultyRepository = facultyRepository;
        this.userProfileRepository = userProfileRepository;
        this.documentRepository = documentRepository;
        this.postRepository = postRepository;
    }

    // ===================== DTO (dữ liệu gửi lên / trả về) =====================

    /** Dữ liệu gửi lên khi tạo / sửa trường. */
    public record InstitutionRequest(
            @NotBlank @Size(max = 255) String name,
            @NotBlank @Pattern(regexp = "UNIVERSITY|COMPANY|OTHER") String type) {
    }

    /** Dữ liệu gửi lên khi tạo / sửa khoa. */
    public record FacultyRequest(
            @NotNull @Positive Long institutionId,
            @NotBlank @Size(max = 255) String name) {
    }

    /** Thông tin trường trả về. */
    public record InstitutionInfo(Long id, String name, String type, long facultyCount) {
    }

    /** Thông tin khoa trả về. */
    public record FacultyInfo(Long id, Long institutionId, String name) {
    }

    // =========================================================
    // TRƯỜNG / TỔ CHỨC
    // =========================================================

    @Transactional(readOnly = true)
    public List<InstitutionInfo> list() {
        List<InstitutionInfo> result = new ArrayList<>();
        for (Institution institution : institutionRepository.findAllByOrderByNameAsc()) {
            result.add(toInfo(institution));
        }
        return result;
    }

    public InstitutionInfo create(InstitutionRequest request) {
        String name = request.name().trim();
        if (institutionRepository.existsByNameIgnoreCase(name)) {
            throw conflict("Tên trường/tổ chức đã tồn tại");
        }

        Institution institution = new Institution();
        institution.setName(name);
        institution.setType(request.type());
        institutionRepository.saveAndFlush(institution);
        return toInfo(institution);
    }

    public InstitutionInfo update(Long id, InstitutionRequest request) {
        Institution institution = findInstitution(id);
        String name = request.name().trim();

        // Tên mới không được trùng với trường khác
        if (institutionRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw conflict("Tên trường/tổ chức đã tồn tại");
        }

        // Nếu đổi loại trường
        boolean typeChanged = !institution.getType().equals(request.type());
        if (typeChanged) {
            protectLastOther(institution);

            // Đổi sang loại "Khác" thì trường không được còn khoa
            if (InstitutionType.OTHER.is(request.type()) && facultyRepository.countByInstitutionId(id) > 0) {
                throw conflict("Hãy xóa hoặc chuyển các khoa trước khi đổi loại thành Khác");
            }
        }

        institution.setName(name);
        institution.setType(request.type());
        institutionRepository.flush();
        return toInfo(institution);
    }

    public void delete(Long id) {
        Institution institution = findInstitution(id);
        protectLastOther(institution);

        if (facultyRepository.countByInstitutionId(id) > 0) {
            throw conflict("Trường còn khoa trực thuộc. Hãy xử lý các khoa trước");
        }

        boolean inUse = userProfileRepository.existsByInstitutionId(id)
                || documentRepository.existsByInstitutionId(id)
                || postRepository.existsByInstitutionId(id);
        if (inUse) {
            throw conflict("Trường đang được hồ sơ hoặc tài liệu/bài viết sử dụng, chưa thể xóa");
        }

        institutionRepository.delete(institution);
        institutionRepository.flush();
    }

    // =========================================================
    // KHOA
    // =========================================================

    @Transactional(readOnly = true)
    public List<FacultyInfo> faculties(Long institutionId) {
        findInstitution(institutionId); // Kiểm tra trường có tồn tại không

        List<FacultyInfo> result = new ArrayList<>();
        for (Faculty faculty : facultyRepository.findByInstitutionIdOrderByNameAsc(institutionId)) {
            result.add(toInfo(faculty));
        }
        return result;
    }

    public FacultyInfo createFaculty(FacultyRequest request) {
        Institution institution = findParentInstitution(request.institutionId());
        String name = request.name().trim();

        if (facultyRepository.existsByInstitutionIdAndNameIgnoreCase(institution.getId(), name)) {
            throw conflict("Khoa đã tồn tại trong trường này");
        }

        Faculty faculty = new Faculty();
        faculty.setInstitution(institution);
        faculty.setName(name);
        facultyRepository.saveAndFlush(faculty);
        return toInfo(faculty);
    }

    public FacultyInfo updateFaculty(Long id, FacultyRequest request) {
        Faculty faculty = findFaculty(id);
        Institution newInstitution = findParentInstitution(request.institutionId());
        String name = request.name().trim();

        // Khoa đang được sử dụng thì không được chuyển sang trường khác
        boolean movingToOtherInstitution = !faculty.getInstitution().getId().equals(newInstitution.getId());
        if (movingToOtherInstitution && isFacultyInUse(id)) {
            throw conflict("Khoa đang được sử dụng, không thể chuyển sang trường khác");
        }

        if (facultyRepository.existsByInstitutionIdAndNameIgnoreCaseAndIdNot(newInstitution.getId(), name, id)) {
            throw conflict("Khoa đã tồn tại trong trường này");
        }

        faculty.setInstitution(newInstitution);
        faculty.setName(name);
        facultyRepository.flush();
        return toInfo(faculty);
    }

    public void deleteFaculty(Long id) {
        Faculty faculty = findFaculty(id);
        if (isFacultyInUse(id)) {
            throw conflict("Khoa đang được hồ sơ hoặc tài liệu/bài viết sử dụng, chưa thể xóa");
        }
        facultyRepository.delete(faculty);
        facultyRepository.flush();
    }

    // =========================================================
    // HÀM HỖ TRỢ
    // =========================================================

    private boolean isFacultyInUse(Long facultyId) {
        return userProfileRepository.existsByFacultyId(facultyId)
                || documentRepository.existsByFacultyId(facultyId)
                || postRepository.existsByFacultyId(facultyId);
    }

    /** Tìm trường để gắn khoa vào. Trường loại "Khác" không được có khoa. */
    private Institution findParentInstitution(Long institutionId) {
        Institution institution = findInstitution(institutionId);
        if (InstitutionType.OTHER.is(institution.getType())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Tổ chức loại Khác không có khoa trực thuộc");
        }
        return institution;
    }

    /** Không cho xoá / đổi loại tổ chức "Khác" cuối cùng. */
    private void protectLastOther(Institution institution) {
        boolean isOther = InstitutionType.OTHER.is(institution.getType());
        if (isOther && institutionRepository.countByType(InstitutionType.OTHER.name()) <= 1) {
            throw conflict("Cần giữ ít nhất một lựa chọn Khác cho người dùng");
        }
    }

    private Institution findInstitution(Long id) {
        return institutionRepository.findById(id)
                .orElseThrow(() -> notFound("Không tìm thấy trường/tổ chức"));
    }

    private Faculty findFaculty(Long id) {
        return facultyRepository.findById(id)
                .orElseThrow(() -> notFound("Không tìm thấy khoa"));
    }

    private InstitutionInfo toInfo(Institution institution) {
        long facultyCount = facultyRepository.countByInstitutionId(institution.getId());
        return new InstitutionInfo(institution.getId(), institution.getName(), institution.getType(), facultyCount);
    }

    private FacultyInfo toInfo(Faculty faculty) {
        return new FacultyInfo(faculty.getId(), faculty.getInstitution().getId(), faculty.getName());
    }

    private ApiException conflict(String message) {
        return new ApiException(HttpStatus.CONFLICT, message);
    }

    private ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, message);
    }
}
