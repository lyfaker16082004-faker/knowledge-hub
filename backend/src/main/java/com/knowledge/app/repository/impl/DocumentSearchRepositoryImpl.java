package com.knowledge.app.repository.impl;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.knowledge.app.model.entity.Document;
import com.knowledge.app.common.util.TextUtils;
import com.knowledge.app.common.util.TimeUtils;
import com.knowledge.app.repository.DocumentRepository;
import com.knowledge.app.repository.custom.DocumentSearchRepository;

/**
 * Tìm kiếm tài liệu bằng SQL thuần (JdbcTemplate).
 *
 * Cách làm:
 * 1. Ghép câu SQL theo các bộ lọc người dùng chọn
 * 2. Đếm tổng số kết quả
 * 3. Lấy danh sách ID của trang hiện tại
 * 4. Dùng JPA tải đầy đủ Document theo các ID đó (giữ đúng thứ tự)
 *
 * Nếu database là MySQL thì dùng thêm tìm kiếm FULLTEXT (MATCH ... AGAINST).
 */
@Repository
public class DocumentSearchRepositoryImpl implements DocumentSearchRepository {

    private static final String TAG_LIKE_CONDITION =
            "EXISTS (SELECT 1 FROM document_tags dt JOIN tags t ON t.id = dt.tag_id"
                    + " WHERE dt.document_id = d.id AND LOWER(t.name) LIKE ? ESCAPE '!')";

    private static final String TAG_EQUAL_CONDITION =
            "EXISTS (SELECT 1 FROM document_tags dt JOIN tags t ON t.id = dt.tag_id"
                    + " WHERE dt.document_id = d.id AND LOWER(t.name) = ?)";

    private final JdbcTemplate jdbcTemplate;
    private final DocumentRepository documentRepository;
    private final boolean isMySql;

    public DocumentSearchRepositoryImpl(JdbcTemplate jdbcTemplate, DocumentRepository documentRepository, DataSource dataSource)
            throws SQLException {
        this.jdbcTemplate = jdbcTemplate;
        this.documentRepository = documentRepository;

        // Kiểm tra đang dùng MySQL hay database khác (VD: H2 khi chạy test)
        try (Connection connection = dataSource.getConnection()) {
            String databaseName = connection.getMetaData().getDatabaseProductName();
            this.isMySql = databaseName.equalsIgnoreCase("MySQL");
        }
    }

    @Override
    public Page<Document> search(String keyword, Long categoryId, String accessType, Long institutionId,
            Long facultyId, Long subjectId, String tag, String order, int page, int size) {

        // Danh sách giá trị cho các dấu "?" trong câu SQL (theo đúng thứ tự)
        List<Object> params = new ArrayList<>();

        // ---- Bước 1: Ghép phần FROM ... WHERE ----
        StringBuilder where = new StringBuilder();
        where.append(" FROM documents d JOIN categories c ON c.id = d.category_id");
        where.append(" WHERE d.status = 'PUBLISHED' AND c.active = true");

        addEqualFilter(where, params, "d.category_id", categoryId);
        addEqualFilter(where, params, "d.institution_id", institutionId);
        addEqualFilter(where, params, "d.faculty_id", facultyId);
        addEqualFilter(where, params, "d.subject_id", subjectId);

        if (!accessType.isBlank()) {
            where.append(" AND d.access_type = ?");
            params.add(accessType);
        }

        // Tìm theo từ khoá trong tiêu đề, tóm tắt, mô tả hoặc nhãn
        if (!keyword.isBlank()) {
            where.append(" AND (");

            if (isMySql) {
                where.append("MATCH(d.title, d.summary, d.description) AGAINST (? IN NATURAL LANGUAGE MODE) > 0 OR ");
                params.add(keyword);
            }

            where.append("LOWER(d.title) LIKE ? ESCAPE '!'");
            where.append(" OR LOWER(d.summary) LIKE ? ESCAPE '!'");
            where.append(" OR LOWER(d.description) LIKE ? ESCAPE '!'");
            where.append(" OR ").append(TAG_LIKE_CONDITION);
            where.append(")");

            String pattern = "%" + TextUtils.escapeLike(keyword.toLowerCase(Locale.ROOT)) + "%";
            for (int i = 0; i < 4; i++) {
                params.add(pattern); // 4 dấu "?" ở trên dùng chung 1 giá trị
            }
        }

        // Lọc theo đúng một nhãn
        if (tag != null && !tag.isBlank()) {
            where.append(" AND ").append(TAG_EQUAL_CONDITION);
            params.add(tag.trim().toLowerCase(Locale.ROOT));
        }

        // ---- Bước 2: Đếm tổng số kết quả ----
        Long total = jdbcTemplate.queryForObject("SELECT COUNT(*)" + where, Long.class, params.toArray());

        // ---- Bước 3: Lấy ID của trang hiện tại ----
        String orderBy = getOrderBy(order);
        if (order.equals("popular")) {
            // Phổ biến = nhiều lượt tải nhất trong 7 ngày gần đây
            LocalDateTime sevenDaysAgo = TimeUtils.now().minusDays(7);
            params.add(Timestamp.valueOf(sevenDaysAgo));
        }
        params.add(size);              // LIMIT
        params.add((long) page * size); // OFFSET

        String idQuery = "SELECT d.id" + where + " ORDER BY " + orderBy + " LIMIT ? OFFSET ?";
        List<Long> ids = jdbcTemplate.query(idQuery, (row, rowNumber) -> row.getLong(1), params.toArray());

        // ---- Bước 4: Tải Document theo ID và giữ đúng thứ tự ----
        Map<Long, Document> documentsById = new HashMap<>();
        for (Document document : documentRepository.findAllById(ids)) {
            documentsById.put(document.getId(), document);
        }

        List<Document> orderedDocuments = new ArrayList<>();
        for (Long id : ids) {
            orderedDocuments.add(documentsById.get(id));
        }

        long totalCount = total != null ? total : 0;
        return new PageImpl<>(orderedDocuments, PageRequest.of(page, size), totalCount);
    }

    /** Thêm điều kiện "AND cột = ?" nếu giá trị khác null. */
    private void addEqualFilter(StringBuilder where, List<Object> params, String column, Long value) {
        if (value != null) {
            where.append(" AND ").append(column).append(" = ?");
            params.add(value);
        }
    }

    /** Chọn cách sắp xếp. */
    private String getOrderBy(String order) {
        switch (order) {
            case "title":
                return "d.title ASC, d.id DESC";
            case "featured":
                return "d.featured DESC, d.published_at DESC, d.id DESC";
            case "popular":
                return "(SELECT COUNT(*) FROM document_downloads dl"
                        + " WHERE dl.document_id = d.id AND dl.created_at >= ?) DESC, d.id DESC";
            default:
                return "d.published_at DESC, d.id DESC"; // Mới nhất
        }
    }
}
