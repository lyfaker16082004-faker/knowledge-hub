package com.knowledge.app.controller.document;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.knowledge.app.common.exception.ApiException;
import com.knowledge.app.common.response.ApiResponse;
import com.knowledge.app.common.response.PageResponse;
import com.knowledge.app.model.dto.request.document.UploadDocumentRequest;
import com.knowledge.app.model.dto.response.document.DocumentDetail;
import com.knowledge.app.model.dto.response.document.DocumentSummary;
import com.knowledge.app.model.dto.response.document.LibraryResponses.CatalogueStats;
import com.knowledge.app.model.dto.response.document.LibraryResponses.CategoryInfo;
import com.knowledge.app.model.dto.response.document.LibraryResponses.DownloadInfo;
import com.knowledge.app.model.dto.response.document.LibraryResponses.PersonalStats;
import com.knowledge.app.security.user.AccountPrincipal;
import com.knowledge.app.service.document.ArchivePreview;
import com.knowledge.app.service.document.DocumentService;
import com.knowledge.app.service.document.DocumentService.DownloadPayload;
import com.knowledge.app.service.document.OfficeTextPreview;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * API thư viện tài liệu: xem danh sách, chi tiết, tải về, xem trước, lưu tài liệu.
 */
@RestController
public class DocumentController {

    private final DocumentService documentService;
    private final OfficeTextPreview officeTextPreview;

    public DocumentController(DocumentService documentService, OfficeTextPreview officeTextPreview) {
        this.documentService = documentService;
        this.officeTextPreview = officeTextPreview;
    }

    // ===================== DTO nhận từ client =====================

    public record BookmarkRequest(@NotNull Boolean saved) {
    }

    public record GrantRequest(@NotNull @Positive Long userId) {
    }

    // =========================================================
    // DANH SÁCH / CHI TIẾT
    // =========================================================

    /** GET /api/documents - Danh sách tài liệu theo bộ lọc. */
    @GetMapping("/api/documents")
    public ApiResponse<PageResponse<DocumentSummary>> list(
            @RequestParam(defaultValue = "") @Size(max = 150) String q,
            @RequestParam(required = false) Long category,
            @RequestParam(defaultValue = "") @Pattern(regexp = "|FREE|PAID") String access,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "9") @Min(1) @Max(30) int size,
            @RequestParam(defaultValue = "newest") @Pattern(regexp = "newest|title|featured|popular") String sort,
            @RequestParam(required = false) Long institutionId,
            @RequestParam(required = false) Long facultyId,
            @RequestParam(defaultValue = "false") boolean global,
            @RequestParam(required = false) @Positive Long subjectId,
            @RequestParam(defaultValue = "") @Size(max = 80) String tag,
            @AuthenticationPrincipal AccountPrincipal principal) {
        PageResponse<DocumentSummary> result = documentService.catalogue(q, category, access, page, size, sort,
                institutionId, facultyId, global, AccountPrincipal.idOf(principal), subjectId, tag);
        return ApiResponse.ok(result);
    }

    /** GET /api/documents/categories - Danh sách chủ đề kèm số lượng tài liệu. */
    @GetMapping("/api/documents/categories")
    public ApiResponse<List<CategoryInfo>> categories() {
        return ApiResponse.ok(documentService.categories());
    }

    /** GET /api/documents/stats - Thống kê chung. */
    @GetMapping("/api/documents/stats")
    public ApiResponse<CatalogueStats> stats() {
        return ApiResponse.ok(documentService.stats());
    }

    /** GET /api/documents/{id} - Chi tiết tài liệu. */
    @GetMapping("/api/documents/{id}")
    public ApiResponse<DocumentDetail> detail(@PathVariable Long id,
            @AuthenticationPrincipal AccountPrincipal principal) {
        return ApiResponse.ok(documentService.detail(id, AccountPrincipal.idOf(principal)));
    }

    /** GET /api/documents/{id}/related - Tài liệu liên quan (mặc định 4 tài liệu). */
    @GetMapping("/api/documents/{id}/related")
    public ApiResponse<List<DocumentSummary>> related(@PathVariable Long id,
            @RequestParam(defaultValue = "4") @Min(1) @Max(12) int limit,
            @AuthenticationPrincipal AccountPrincipal principal) {
        return ApiResponse.ok(documentService.related(id, AccountPrincipal.idOf(principal), limit));
    }

    // =========================================================
    // ĐĂNG TẢI / TẢI VỀ / XEM TRƯỚC
    // =========================================================

    /** POST /api/documents - Đăng tải tài liệu (gửi dạng multipart: metadata JSON + file). */
    @PostMapping(value = "/api/documents", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<DocumentSummary> upload(@AuthenticationPrincipal AccountPrincipal principal,
            @Valid @RequestPart("metadata") UploadDocumentRequest request,
            @RequestPart("file") MultipartFile file) {
        return ApiResponse.ok(documentService.upload(principal.getId(), request, file));
    }

    /** POST /api/documents/{id}/files/{fileId}/download - Tải file về máy. */
    @PostMapping("/api/documents/{id}/files/{fileId}/download")
    public ResponseEntity<byte[]> download(@PathVariable Long id, @PathVariable Long fileId,
            @AuthenticationPrincipal AccountPrincipal principal) {
        DownloadPayload file = documentService.download(id, fileId, principal.getId());

        // "attachment" = trình duyệt sẽ tải file về thay vì mở trực tiếp
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(file.fileName(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .contentType(MediaType.parseMediaType(file.contentType()))
                .body(file.bytes());
    }

    /** GET /api/documents/{id}/files/{fileId}/sample - Xem thử một phần tài liệu (ai cũng xem được). */
    @GetMapping("/api/documents/{id}/files/{fileId}/sample")
    public ResponseEntity<byte[]> sample(@PathVariable Long id, @PathVariable Long fileId,
            @AuthenticationPrincipal AccountPrincipal principal) {
        DownloadPayload file = documentService.sample(AccountPrincipal.idOf(principal), id, fileId);
        return safeFileResponse(file, true);
    }

    /** GET /api/documents/{id}/files/{fileId}/preview - Tác giả / admin xem bản đầy đủ để kiểm duyệt. */
    @GetMapping("/api/documents/{id}/files/{fileId}/preview")
    public ResponseEntity<byte[]> preview(@PathVariable Long id, @PathVariable Long fileId,
            @AuthenticationPrincipal AccountPrincipal principal) {
        DownloadPayload file = documentService.preview(principal.getId(), id, fileId);

        // PDF và file văn bản thì mở trực tiếp trên trình duyệt, loại khác thì tải về
        boolean showInBrowser = file.contentType().startsWith("application/pdf")
                || file.contentType().startsWith("text/");
        return safeFileResponse(file, showInBrowser);
    }

    /** GET /api/documents/{id}/files/{fileId}/preview-text - Trích văn bản từ PPTX/DOCX hoặc liệt kê file trong ZIP/RAR. */
    @GetMapping("/api/documents/{id}/files/{fileId}/preview-text")
    public ApiResponse<String> officePreview(@PathVariable Long id, @PathVariable Long fileId,
            @AuthenticationPrincipal AccountPrincipal principal) {
        DownloadPayload file = documentService.preview(principal.getId(), id, fileId);
        String fileName = file.fileName().toLowerCase(Locale.ROOT);

        if (fileName.matches(".*[.](zip|rar)")) {
            return ApiResponse.ok(ArchivePreview.describe(file.bytes(), file.fileName()));
        }
        if (fileName.matches(".*[.](pptx|docx)")) {
            return ApiResponse.ok(officeTextPreview.extract(file.bytes()));
        }
        throw new ApiException(HttpStatus.BAD_REQUEST, "Chỉ trích văn bản cho PPTX, DOCX, ZIP và RAR");
    }

    // =========================================================
    // THƯ VIỆN CÁ NHÂN
    // =========================================================

    /** GET /api/users/me/library/stats - Thống kê cá nhân. */
    @GetMapping("/api/users/me/library/stats")
    public ApiResponse<PersonalStats> stats(@AuthenticationPrincipal AccountPrincipal principal) {
        return ApiResponse.ok(documentService.personalStats(principal.getId()));
    }

    /** GET /api/users/me/downloads - Lịch sử tải. */
    @GetMapping("/api/users/me/downloads")
    public ApiResponse<PageResponse<DownloadInfo>> history(@AuthenticationPrincipal AccountPrincipal principal,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(30) int size) {
        return ApiResponse.ok(documentService.history(principal.getId(), page, size));
    }

    /** GET /api/users/me/bookmarks - Tài liệu đã lưu. */
    @GetMapping("/api/users/me/bookmarks")
    public ApiResponse<PageResponse<DocumentSummary>> saved(@AuthenticationPrincipal AccountPrincipal principal,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "9") @Min(1) @Max(30) int size) {
        return ApiResponse.ok(documentService.saved(principal.getId(), page, size));
    }

    /** POST /api/users/me/bookmarks/{id} - Lưu / bỏ lưu tài liệu. */
    @PostMapping("/api/users/me/bookmarks/{id}")
    public ApiResponse<Void> bookmark(@PathVariable Long id, @AuthenticationPrincipal AccountPrincipal principal,
            @Valid @RequestBody BookmarkRequest request) {
        documentService.bookmark(principal.getId(), id, request.saved());
        return ApiResponse.ok(null);
    }

    /** POST /api/admin/documents/{id}/access - Admin cấp quyền đọc tài liệu có phí cho người dùng. */
    @PostMapping("/api/admin/documents/{id}/access")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> grant(@PathVariable Long id, @Valid @RequestBody GrantRequest request) {
        documentService.grant(id, request.userId());
        return ApiResponse.ok(null);
    }

    // =========================================================
    // HÀM HỖ TRỢ
    // =========================================================


    /**
     * Trả file về trình duyệt kèm các header bảo mật:
     * - nosniff: trình duyệt không được tự đoán loại file
     * - sandbox: không cho chạy script trong file
     */
    private ResponseEntity<byte[]> safeFileResponse(DownloadPayload file, boolean showInBrowser) {
        ContentDisposition.Builder builder = showInBrowser
                ? ContentDisposition.inline()
                : ContentDisposition.attachment();
        ContentDisposition disposition = builder.filename(file.fileName(), StandardCharsets.UTF_8).build();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .header("X-Content-Type-Options", "nosniff")
                .header("Content-Security-Policy", "sandbox; default-src 'none'")
                .contentType(MediaType.parseMediaType(file.contentType()))
                .body(file.bytes());
    }
}
