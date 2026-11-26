package com.knowledge.app.model.dto.response.document;

import java.util.List;

/**
 * Thông tin chi tiết một tài liệu (trang xem tài liệu).
 */
public record DocumentDetail(
        DocumentSummary document,   // Thông tin tóm tắt
        String description,         // Mô tả đầy đủ
        String author,              // Tên tác giả
        boolean saved,              // Người xem đã lưu chưa
        boolean canDownload,        // Người xem có được tải không
        List<FileInfo> files,       // Danh sách file đính kèm
        String status,              // Trạng thái (PENDING, PUBLISHED, ...)
        String reviewNote,          // Ghi chú duyệt (chỉ người quản lý thấy)
        Long version,               // Phiên bản (chỉ người quản lý thấy)
        boolean canManage,          // Được sửa / xoá không
        boolean canPreview) {       // Được xem bản kiểm duyệt không

    /** Thông tin một file đính kèm. */
    public record FileInfo(Long id, String name, String contentType, long sizeBytes) {
    }
}
