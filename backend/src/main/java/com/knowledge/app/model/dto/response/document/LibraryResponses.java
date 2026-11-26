package com.knowledge.app.model.dto.response.document;

import java.time.LocalDateTime;

/**
 * Gom các DTO nhỏ của thư viện tài liệu vào một chỗ.
 */
public final class LibraryResponses {

    private LibraryResponses() {
        // Không cho tạo đối tượng, chỉ dùng để chứa các record bên dưới
    }

    /** Chủ đề kèm số lượng tài liệu. */
    public record CategoryInfo(Long id, String name, String slug, long count) {
    }

    /** Thống kê chung của thư viện. */
    public record CatalogueStats(long documents, long freeDocuments, long categories, long downloads) {
    }

    /** Thống kê cá nhân. */
    public record PersonalStats(long downloadedDocuments, long downloads, long savedDocuments) {
    }

    /** Một dòng trong lịch sử tải. */
    public record DownloadInfo(Long id, Long documentId, String title, String category, String fileName,
            LocalDateTime downloadedAt, boolean available) {
    }
}
