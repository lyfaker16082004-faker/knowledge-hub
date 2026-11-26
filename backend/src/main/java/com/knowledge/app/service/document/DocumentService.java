package com.knowledge.app.service.document;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.knowledge.app.common.response.PageResponse;
import com.knowledge.app.model.dto.request.document.EditDocumentRequest;
import com.knowledge.app.model.dto.request.document.UploadDocumentRequest;
import com.knowledge.app.model.dto.response.document.DocumentDetail;
import com.knowledge.app.model.dto.response.document.DocumentSummary;
import com.knowledge.app.model.dto.response.document.LibraryResponses.CatalogueStats;
import com.knowledge.app.model.dto.response.document.LibraryResponses.CategoryInfo;
import com.knowledge.app.model.dto.response.document.LibraryResponses.DownloadInfo;
import com.knowledge.app.model.dto.response.document.LibraryResponses.PersonalStats;
import com.knowledge.app.model.dto.response.document.ManagedDocument;

/**
 * Các chức năng của thư viện tài liệu.
 * Code xử lý nằm trong DocumentServiceImpl.
 */
public interface DocumentService {

    /** Dữ liệu file trả về khi tải / xem trước. */
    record DownloadPayload(byte[] bytes, String fileName, String contentType) {
    }

    // ===================== Xem tài liệu =====================

    /** Danh sách tài liệu theo bộ lọc. */
    PageResponse<DocumentSummary> catalogue(String q, Long category, String access, int page, int size,
            String sort, Long institutionId, Long facultyId, boolean global, Long userId, Long subjectId,
            String tag);

    /** Danh sách chủ đề kèm số lượng tài liệu. */
    List<CategoryInfo> categories();

    /** Thống kê chung của thư viện. */
    CatalogueStats stats();

    /** Chi tiết một tài liệu. */
    DocumentDetail detail(Long id, Long userId);

    /** Tài liệu liên quan (cùng môn học, cùng nhãn, cùng chủ đề...) để gợi ý ở cuối trang chi tiết. */
    List<DocumentSummary> related(Long documentId, Long viewerId, int limit);

    /** Tải file của tài liệu. */
    DownloadPayload download(Long documentId, Long fileId, Long userId);

    /** Bản xem đầy đủ để kiểm duyệt (tác giả / admin). */
    DownloadPayload preview(Long viewer, Long documentId, Long fileId);

    /** Bản xem thử (một phần nội dung). */
    DownloadPayload sample(Long viewer, Long documentId, Long fileId);

    // ===================== Thư viện cá nhân =====================

    PersonalStats personalStats(Long userId);

    PageResponse<DownloadInfo> history(Long userId, int page, int size);

    PageResponse<DocumentSummary> saved(Long userId, int page, int size);

    void bookmark(Long userId, Long documentId, boolean saved);

    /** Admin cấp quyền đọc tài liệu có phí. */
    void grant(Long documentId, Long userId);

    // ===================== Đăng tải và quản lý =====================

    DocumentSummary upload(Long userId, UploadDocumentRequest request, MultipartFile file);

    PageResponse<ManagedDocument> managed(Long userId, boolean admin, String q, String status, int page, int size);

    ManagedDocument update(Long userId, boolean admin, Long id, EditDocumentRequest request);

    void delete(Long userId, boolean admin, Long id, Long version);

    /** Admin duyệt tài liệu: APPROVE, REJECT, HIDE, RESTORE. */
    ManagedDocument review(Long adminId, Long id, String action, String note, Long version);
}
