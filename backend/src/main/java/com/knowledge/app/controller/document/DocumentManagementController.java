package com.knowledge.app.controller.document;
import com.knowledge.app.service.document.DocumentService;
import com.knowledge.app.model.dto.request.document.EditDocumentRequest;
import com.knowledge.app.model.dto.response.document.ManagedDocument;
import com.knowledge.app.common.response.*;
import com.knowledge.app.security.user.AccountPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
@RestController public class DocumentManagementController {
 private final DocumentService docs;public DocumentManagementController(DocumentService docs){this.docs=docs;}
 @GetMapping("/api/users/me/documents") public ApiResponse<PageResponse<ManagedDocument>> mine(@AuthenticationPrincipal AccountPrincipal user,@RequestParam(defaultValue="") @Size(max=150) String q,@RequestParam(defaultValue="") @Pattern(regexp="|DRAFT|PENDING|PUBLISHED|REJECTED|ARCHIVED") String status,@RequestParam(defaultValue="0") @Min(0) int page,@RequestParam(defaultValue="10") @Min(1) @Max(30) int size){return ApiResponse.ok(docs.managed(user.getId(),false,q,status,page,size));}
 @GetMapping("/api/admin/documents") public ApiResponse<PageResponse<ManagedDocument>> all(@AuthenticationPrincipal AccountPrincipal user,@RequestParam(defaultValue="") @Size(max=150) String q,@RequestParam(defaultValue="") @Pattern(regexp="|DRAFT|PENDING|PUBLISHED|REJECTED|ARCHIVED") String status,@RequestParam(defaultValue="0") @Min(0) int page,@RequestParam(defaultValue="10") @Min(1) @Max(30) int size){return ApiResponse.ok(docs.managed(user.getId(),true,q,status,page,size));}
 @PutMapping("/api/users/me/documents/{id}") public ApiResponse<ManagedDocument> edit(@AuthenticationPrincipal AccountPrincipal user,@PathVariable Long id,@Valid @RequestBody EditDocumentRequest request){return ApiResponse.ok(docs.update(user.getId(),false,id,request));}
 @PutMapping("/api/admin/documents/{id}") public ApiResponse<ManagedDocument> editAdmin(@AuthenticationPrincipal AccountPrincipal user,@PathVariable Long id,@Valid @RequestBody EditDocumentRequest request){return ApiResponse.ok(docs.update(user.getId(),true,id,request));}
 @DeleteMapping("/api/users/me/documents/{id}") public ApiResponse<Void> remove(@AuthenticationPrincipal AccountPrincipal user,@PathVariable Long id,@RequestParam @PositiveOrZero Long version){docs.delete(user.getId(),false,id,version);return ApiResponse.ok(null);}
 @DeleteMapping("/api/admin/documents/{id}") public ApiResponse<Void> removeAdmin(@AuthenticationPrincipal AccountPrincipal user,@PathVariable Long id,@RequestParam @PositiveOrZero Long version){docs.delete(user.getId(),true,id,version);return ApiResponse.ok(null);}
 @PostMapping("/api/admin/documents/{id}/review") public ApiResponse<ManagedDocument> review(@AuthenticationPrincipal AccountPrincipal user,@PathVariable Long id,@Valid @RequestBody ReviewRequest request){return ApiResponse.ok(docs.review(user.getId(),id,request.action(),request.note(),request.version()));}
 public record ReviewRequest(@NotNull @PositiveOrZero Long version,@NotBlank @Pattern(regexp="APPROVE|REJECT|HIDE|RESTORE") String action,@Size(max=2000) String note){}
}
