package com.knowledge.app.service.organization;
import com.knowledge.app.model.entity.*;
import com.knowledge.app.model.dto.response.user.OrganizationInfo;
import com.knowledge.app.repository.*;
import com.knowledge.app.common.exception.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
@Service @Transactional(readOnly=true)
public class OrganizationService {
 private final InstitutionRepository institutions;private final FacultyRepository faculties;
 public OrganizationService(InstitutionRepository institutions,FacultyRepository faculties){this.institutions=institutions;this.faculties=faculties;}
 public Selection resolve(Long institutionId,Long facultyId){
  if(institutionId==null){if(facultyId!=null)throw invalid("Cần chọn trường/tổ chức trước khi chọn khoa");return new Selection(null,null);}
  var school=institutions.findById(institutionId).orElseThrow(()->invalid("Trường/tổ chức không tồn tại"));
  if("OTHER".equals(school.getType())&&facultyId!=null)throw invalid("Lựa chọn Khác không sử dụng khoa/chuyên ngành");
  var faculty=facultyId==null?null:faculties.findByIdAndInstitutionId(facultyId,institutionId).orElseThrow(()->invalid("Khoa không thuộc trường/tổ chức đã chọn"));
  return new Selection(school,faculty);
 }
 public OrganizationInfo info(Institution school,Faculty faculty){return school==null?null:new OrganizationInfo(school.getId(),school.getName(),school.getType(),faculty==null?null:faculty.getId(),faculty==null?null:faculty.getName());}
 private ApiException invalid(String message){return new ApiException(HttpStatus.BAD_REQUEST,message);}
 public record Selection(Institution institution,Faculty faculty){}
}
