package com.knowledge.app.service.organization;
import com.knowledge.app.repository.*;
import com.knowledge.app.model.entity.*;
import com.knowledge.app.common.exception.ApiException;
import jakarta.validation.constraints.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import java.util.*;
@Service @Transactional
public class OrganizationAdminService {
 private final InstitutionRepository institutions;private final FacultyRepository faculties;private final UserProfileRepository profiles;private final DocumentRepository docs;private final PostRepository posts;
 public OrganizationAdminService(InstitutionRepository institutions,FacultyRepository faculties,UserProfileRepository profiles,DocumentRepository docs,PostRepository posts){this.institutions=institutions;this.faculties=faculties;this.profiles=profiles;this.docs=docs;this.posts=posts;}
 public record InstitutionRequest(@NotBlank @Size(max=255) String name,@NotBlank @Pattern(regexp="UNIVERSITY|COMPANY|OTHER") String type){}
 public record FacultyRequest(@NotNull @Positive Long institutionId,@NotBlank @Size(max=255) String name){}
 public record InstitutionInfo(Long id,String name,String type,long facultyCount){}
 public record FacultyInfo(Long id,Long institutionId,String name){}
 @Transactional(readOnly=true) public List<InstitutionInfo> list(){return institutions.findAllByOrderByNameAsc().stream().map(this::info).toList();}
 @Transactional(readOnly=true) public List<FacultyInfo> faculties(Long id){required(id);return faculties.findByInstitutionIdOrderByNameAsc(id).stream().map(this::info).toList();}
 public InstitutionInfo create(InstitutionRequest request){String name=request.name().trim();if(institutions.existsByNameIgnoreCase(name))throw conflict("Tên trường/tổ chức đã tồn tại");var school=new Institution();school.setName(name);school.setType(request.type());institutions.saveAndFlush(school);return info(school);}
 public InstitutionInfo update(Long id,InstitutionRequest request){var school=required(id);String name=request.name().trim();if(institutions.existsByNameIgnoreCaseAndIdNot(name,id))throw conflict("Tên trường/tổ chức đã tồn tại");if(!school.getType().equals(request.type())){protectLastOther(school);if("OTHER".equals(request.type())&&faculties.countByInstitutionId(id)>0)throw conflict("Hãy xóa hoặc chuyển các khoa trước khi đổi loại thành Khác");}school.setName(name);school.setType(request.type());institutions.flush();return info(school);}
 public void delete(Long id){var school=required(id);protectLastOther(school);if(faculties.countByInstitutionId(id)>0)throw conflict("Trường còn khoa trực thuộc. Hãy xử lý các khoa trước");if(profiles.existsByInstitutionId(id)||docs.existsByInstitutionId(id)||posts.existsByInstitutionId(id))throw conflict("Trường đang được hồ sơ hoặc tài liệu/bài viết sử dụng, chưa thể xóa");institutions.delete(school);institutions.flush();}
 public FacultyInfo createFaculty(FacultyRequest request){var school=validParent(request.institutionId());String name=request.name().trim();if(faculties.existsByInstitutionIdAndNameIgnoreCase(school.getId(),name))throw conflict("Khoa đã tồn tại trong trường này");var faculty=new Faculty();faculty.setInstitution(school);faculty.setName(name);faculties.saveAndFlush(faculty);return info(faculty);}
 public FacultyInfo updateFaculty(Long id,FacultyRequest request){var faculty=faculties.findById(id).orElseThrow(()->missing("Không tìm thấy khoa"));var school=validParent(request.institutionId());String name=request.name().trim();if(!faculty.getInstitution().getId().equals(school.getId())&&facultyUsed(id))throw conflict("Khoa đang được sử dụng, không thể chuyển sang trường khác");if(faculties.existsByInstitutionIdAndNameIgnoreCaseAndIdNot(school.getId(),name,id))throw conflict("Khoa đã tồn tại trong trường này");faculty.setInstitution(school);faculty.setName(name);faculties.flush();return info(faculty);}
 public void deleteFaculty(Long id){var faculty=faculties.findById(id).orElseThrow(()->missing("Không tìm thấy khoa"));if(facultyUsed(id))throw conflict("Khoa đang được hồ sơ hoặc tài liệu/bài viết sử dụng, chưa thể xóa");faculties.delete(faculty);faculties.flush();}
 private boolean facultyUsed(Long id){return profiles.existsByFacultyId(id)||docs.existsByFacultyId(id)||posts.existsByFacultyId(id);}
 private Institution validParent(Long id){var school=required(id);if("OTHER".equals(school.getType()))throw new ApiException(HttpStatus.BAD_REQUEST,"Tổ chức loại Khác không có khoa trực thuộc");return school;}
 private void protectLastOther(Institution school){if("OTHER".equals(school.getType())&&institutions.countByType("OTHER")<=1)throw conflict("Cần giữ ít nhất một lựa chọn Khác cho người dùng");}
 private Institution required(Long id){return institutions.findById(id).orElseThrow(()->missing("Không tìm thấy trường/tổ chức"));}
 private InstitutionInfo info(Institution i){return new InstitutionInfo(i.getId(),i.getName(),i.getType(),faculties.countByInstitutionId(i.getId()));}
 private FacultyInfo info(Faculty f){return new FacultyInfo(f.getId(),f.getInstitution().getId(),f.getName());}
 private ApiException conflict(String text){return new ApiException(HttpStatus.CONFLICT,text);}
 private ApiException missing(String text){return new ApiException(HttpStatus.NOT_FOUND,text);}
}
