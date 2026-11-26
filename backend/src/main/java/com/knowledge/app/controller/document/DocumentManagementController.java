package com.knowledge.app.controller.document;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.knowledge.app.common.response.ApiResponse;
import com.knowledge.app.common.response.PageResponse;
import com.knowledge.app.model.dto.request.document.EditDocumentRequest;
import com.knowledge.app.model.dto.response.document.ManagedDocument;
import com.knowledge.app.security.user.AccountPrincipal;
import com.knowledge.app.service.document.DocumentService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * API quản lý tài liệu.
 * - /api/users/me/documents/... : người dùng quản lý tài liệu của mình
 * - /api/admin/documents/...    : admin quản lý tất cả tài liệu
 */
@RestController
public class DocumentManagementController {

    private static final String STATUS_PATTERN = "|DRAFT|PENDING|PUBLISHED|REJECTED|ARCHIVED";

    private final DocumentService documentService;

    public DocumentManagementController(DocumentService documentService) {
        this.documentService = documentService;
    }

    /** Dữ liệu gửi lên khi admin duyệt tài liệu. */
    public record ReviewRequest(
            @NotNull @PositiveOrZero Long version,
            @NotBlank @Pattern(regexp = "APPROVE|REJECT|HIDE|RESTORE") String action,
            @Size(max = 2000) String note) {
    }

    // =========================================================
    // NGƯỜI DÙNG
    // =========================================================

    /** Danh sách tài liệu của tôi. */
    @GetMapping("/api/users/me/documents")
    public ApiResponse<PageResponse<ManagedDocument>> mine(@AuthenticationPrincipal AccountPrincipal user,
            @RequestParam(defaultValue = "") @Size(max = 150) String q,
            @RequestParam(defaultValue = "") @Pattern(regexp = STATUS_PATTERN) String status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(30) int size) {
        return ApiResponse.ok(documentService.managed(user.getId(), false, q, status, page, size));
    }

    /** Sửa tài liệu của tôi. */
    @PutMapping("/api/users/me/documents/{id}")
    public ApiResponse<ManagedDocument> edit(@AuthenticationPrincipal AccountPrincipal user, @PathVariable Long id,
            @Valid @RequestBody EditDocumentRequest request) {
        return ApiResponse.ok(documentService.update(user.getId(), false, id, request));
    }

    /** Xoá tài liệu của tôi. */
    @DeleteMapping("/api/users/me/documents/{id}")
    public ApiResponse<Void> remove(@AuthenticationPrincipal AccountPrincipal user, @PathVariable Long id,
            @RequestParam @PositiveOrZero Long version) {
        documentService.delete(user.getId(), false, id, version);
        return ApiResponse.ok(null);
    }

    // =========================================================
    // ADMIN
    // =========================================================

    /** Admin xem tất cả tài liệu. */
    @GetMapping("/api/admin/documents")
    public ApiResponse<PageResponse<ManagedDocument>> all(@AuthenticationPrincipal AccountPrincipal user,
            @RequestParam(defaultValue = "") @Size(max = 150) String q,
            @RequestParam(defaultValue = "") @Pattern(regexp = STATUS_PATTERN) String status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(30) int size) {
        return ApiResponse.ok(documentService.managed(user.getId(), true, q, status, page, size));
    }

    /** Admin sửa tài liệu bất kỳ. */
    @PutMapping("/api/admin/documents/{id}")
    public ApiResponse<ManagedDocument> editAdmin(@AuthenticationPrincipal AccountPrincipal user,
            @PathVariable Long id, @Valid @RequestBody EditDocumentRequest request) {
        return ApiResponse.ok(documentService.update(user.getId(), true, id, request));
    }

    /** Admin xoá tài liệu bất kỳ. */
    @DeleteMapping("/api/admin/documents/{id}")
    public ApiResponse<Void> removeAdmin(@AuthenticationPrincipal AccountPrincipal user, @PathVariable Long id,
            @RequestParam @PositiveOrZero Long version) {
        documentService.delete(user.getId(), true, id, version);
        return ApiResponse.ok(null);
    }

    /** Admin duyệt / từ chối / ẩn / khôi phục tài liệu. */
    @PostMapping("/api/admin/documents/{id}/review")
    public ApiResponse<ManagedDocument> review(@AuthenticationPrincipal AccountPrincipal user,
            @PathVariable Long id, @Valid @RequestBody ReviewRequest request) {
        return ApiResponse.ok(documentService.review(user.getId(), id, request.action(), request.note(),
                request.version()));
    }
}
