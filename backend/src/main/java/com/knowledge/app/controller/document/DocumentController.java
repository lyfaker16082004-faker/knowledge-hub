package com.knowledge.app.controller.document;
import java.nio.charset.StandardCharsets;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import com.knowledge.app.common.response.*;
import com.knowledge.app.model.dto.response.document.*;
import com.knowledge.app.model.dto.response.document.LibraryResponses.*;
import com.knowledge.app.security.user.AccountPrincipal;
import com.knowledge.app.service.document.DocumentService;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController
public class DocumentController {
 private final com.knowledge.app.service.document.OfficeTextPreview textPreview;
 private final DocumentService docs;
 public DocumentController(DocumentService docs,com.knowledge.app.service.document.OfficeTextPreview textPreview){this.docs=docs;this.textPreview=textPreview;}
 @GetMapping("/api/documents") public ApiResponse<PageResponse<DocumentSummary>> list(@RequestParam(defaultValue="") @Size(max=150) String q,@RequestParam(required=false) Long category,@RequestParam(defaultValue="") @Pattern(regexp="|FREE|PAID") String access,@RequestParam(defaultValue="0") @Min(0) int page,@RequestParam(defaultValue="9") @Min(1) @Max(30) int size,@RequestParam(defaultValue="newest") @Pattern(regexp="newest|title|featured|popular") String sort,@RequestParam(required=false) Long institutionId,@RequestParam(required=false) Long facultyId,@RequestParam(defaultValue="false") boolean global,@RequestParam(required=false) @Positive Long subjectId,@RequestParam(defaultValue="") @Size(max=80) String tag,@AuthenticationPrincipal AccountPrincipal principal){return ApiResponse.ok(docs.catalogue(q,category,access,page,size,sort,institutionId,facultyId,global,principal==null?null:principal.getId(),subjectId,tag));}
 @PostMapping(value="/api/documents",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) @ResponseStatus(HttpStatus.CREATED) public ApiResponse<DocumentSummary> upload(@AuthenticationPrincipal AccountPrincipal principal,@Valid @RequestPart("metadata") com.knowledge.app.model.dto.request.document.UploadDocumentRequest request,@RequestPart("file") org.springframework.web.multipart.MultipartFile file){return ApiResponse.ok(docs.upload(principal.getId(),request,file));}
 @GetMapping("/api/documents/categories") public ApiResponse<List<CategoryInfo>> categories(){return ApiResponse.ok(docs.categories());}
 @GetMapping("/api/documents/stats") public ApiResponse<CatalogueStats> stats(){return ApiResponse.ok(docs.stats());}
 @GetMapping("/api/documents/{id}") public ApiResponse<DocumentDetail> detail(@PathVariable Long id,@AuthenticationPrincipal AccountPrincipal principal){return ApiResponse.ok(docs.detail(id,principal==null?null:principal.getId()));}
 @PostMapping("/api/documents/{id}/files/{fileId}/download") public ResponseEntity<byte[]> download(@PathVariable Long id,@PathVariable Long fileId,@AuthenticationPrincipal AccountPrincipal principal){var result=docs.download(id,fileId,principal.getId());return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename(result.fileName(),StandardCharsets.UTF_8).build().toString()).header(HttpHeaders.CACHE_CONTROL,"no-store").contentType(MediaType.parseMediaType(result.contentType())).body(result.bytes());}
 @GetMapping("/api/users/me/library/stats") public ApiResponse<PersonalStats> stats(@AuthenticationPrincipal AccountPrincipal principal){return ApiResponse.ok(docs.personalStats(principal.getId()));}
 @GetMapping("/api/users/me/downloads") public ApiResponse<PageResponse<DownloadInfo>> history(@AuthenticationPrincipal AccountPrincipal principal,@RequestParam(defaultValue="0") @Min(0) int page,@RequestParam(defaultValue="10") @Min(1) @Max(30) int size){return ApiResponse.ok(docs.history(principal.getId(),page,size));}
 @GetMapping("/api/users/me/bookmarks") public ApiResponse<PageResponse<DocumentSummary>> saved(@AuthenticationPrincipal AccountPrincipal principal,@RequestParam(defaultValue="0") @Min(0) int page,@RequestParam(defaultValue="9") @Min(1) @Max(30) int size){return ApiResponse.ok(docs.saved(principal.getId(),page,size));}
 @PostMapping("/api/users/me/bookmarks/{id}") public ApiResponse<Void> bookmark(@PathVariable Long id,@AuthenticationPrincipal AccountPrincipal principal,@Valid @RequestBody BookmarkRequest request){docs.bookmark(principal.getId(),id,request.saved());return ApiResponse.ok(null);}
 @PostMapping("/api/admin/documents/{id}/access") @PreAuthorize("hasRole('ADMIN')") public ApiResponse<Void> grant(@PathVariable Long id,@Valid @RequestBody GrantRequest request){docs.grant(id,request.userId());return ApiResponse.ok(null);}
 @GetMapping("/api/documents/{id}/files/{fileId}/preview") public ResponseEntity<byte[]> preview(@PathVariable Long id,@PathVariable Long fileId,@AuthenticationPrincipal AccountPrincipal principal){var result=docs.preview(principal.getId(),id,fileId);boolean inline=result.contentType().startsWith("application/pdf")||result.contentType().startsWith("text/");return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,(inline?ContentDisposition.inline():ContentDisposition.attachment()).filename(result.fileName(),StandardCharsets.UTF_8).build().toString()).header(HttpHeaders.CACHE_CONTROL,"no-store").header("X-Content-Type-Options","nosniff").header("Content-Security-Policy","sandbox; default-src 'none'").contentType(MediaType.parseMediaType(result.contentType())).body(result.bytes());}
 @GetMapping("/api/documents/{id}/files/{fileId}/preview-text") public ApiResponse<String> officePreview(@PathVariable Long id,@PathVariable Long fileId,@AuthenticationPrincipal AccountPrincipal principal){var result=docs.preview(principal.getId(),id,fileId);if(!result.fileName().toLowerCase(java.util.Locale.ROOT).matches(".*[.](pptx|docx)"))throw new com.knowledge.app.common.exception.ApiException(HttpStatus.BAD_REQUEST,"Chỉ trích văn bản cho PPTX và DOCX");return ApiResponse.ok(textPreview.extract(result.bytes()));}
 public record BookmarkRequest(@NotNull Boolean saved){}
 public record GrantRequest(@NotNull @Positive Long userId){}
}
