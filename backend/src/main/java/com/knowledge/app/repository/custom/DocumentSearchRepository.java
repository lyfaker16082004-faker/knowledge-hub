package com.knowledge.app.repository.custom;

import org.springframework.data.domain.Page;

import com.knowledge.app.model.entity.Document;

/**
 * Tìm kiếm tài liệu bằng SQL tự viết (không dùng được query method của Spring Data
 * vì điều kiện thay đổi theo bộ lọc và có dùng FULLTEXT của MySQL).
 *
 * Code xử lý nằm trong repository/impl/DocumentSearchRepositoryImpl.
 */
public interface DocumentSearchRepository {

    /**
     * @param keyword       từ khoá (chuỗi rỗng = không lọc)
     * @param accessType    FREE, PAID hoặc chuỗi rỗng
     * @param tag           tên nhãn (null hoặc rỗng = không lọc)
     * @param order         newest, title, featured hoặc popular
     * @param categoryId    các tham số kiểu Long: null = không lọc
     */
    Page<Document> search(String keyword, Long categoryId, String accessType, Long institutionId,
            Long facultyId, Long subjectId, String tag, String order, int page, int size);
}
