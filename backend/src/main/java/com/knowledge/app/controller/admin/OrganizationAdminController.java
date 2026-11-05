package com.knowledge.app.controller.admin;
import com.knowledge.app.service.organization.OrganizationAdminService;
import com.knowledge.app.service.organization.OrganizationAdminService.*;
import com.knowledge.app.common.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/admin") @PreAuthorize("hasRole('ADMIN')")
public class OrganizationAdminController {
 private final OrganizationAdminService service;
 public OrganizationAdminController(OrganizationAdminService service){this.service=service;}
 @GetMapping("/institutions") public ApiResponse<List<InstitutionInfo>> list(){return ApiResponse.ok(service.list());}
 @PostMapping("/institutions") @ResponseStatus(HttpStatus.CREATED) public ApiResponse<InstitutionInfo> create(@Valid @RequestBody InstitutionRequest request){return ApiResponse.ok(service.create(request));}
 @PutMapping("/institutions/{id}") public ApiResponse<InstitutionInfo> update(@PathVariable Long id,@Valid @RequestBody InstitutionRequest request){return ApiResponse.ok(service.update(id,request));}
 @DeleteMapping("/institutions/{id}") public ApiResponse<Void> delete(@PathVariable Long id){service.delete(id);return ApiResponse.ok(null);}
 @GetMapping("/institutions/{id}/faculties") public ApiResponse<List<FacultyInfo>> faculties(@PathVariable Long id){return ApiResponse.ok(service.faculties(id));}
 @PostMapping("/faculties") @ResponseStatus(HttpStatus.CREATED) public ApiResponse<FacultyInfo> createFaculty(@Valid @RequestBody FacultyRequest request){return ApiResponse.ok(service.createFaculty(request));}
 @PutMapping("/faculties/{id}") public ApiResponse<FacultyInfo> updateFaculty(@PathVariable Long id,@Valid @RequestBody FacultyRequest request){return ApiResponse.ok(service.updateFaculty(id,request));}
 @DeleteMapping("/faculties/{id}") public ApiResponse<Void> deleteFaculty(@PathVariable Long id){service.deleteFaculty(id);return ApiResponse.ok(null);}
}
