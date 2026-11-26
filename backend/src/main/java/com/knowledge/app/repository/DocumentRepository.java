package com.knowledge.app.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.knowledge.app.model.entity.Document;

/**
 * Truy vấn bảng tài liệu (Document).
 *
 * @EntityGraph: lấy luôn category, author, institution, faculty trong cùng 1 câu query
 * (tránh lỗi N+1 query khi truy cập các quan hệ này).
 */
public interface DocumentRepository extends JpaRepository<Document, Long>, JpaSpecificationExecutor<Document> {

    /**
     * Tìm tài liệu đã công bố theo bộ lọc.
     * Tham số nào là null (hoặc chuỗi rỗng với access) thì bỏ qua điều kiện đó.
     */
    @EntityGraph(attributePaths = { "category", "author", "institution", "faculty" })
    @Query("""
            select d from Document d
            where d.status = 'PUBLISHED'
              and d.category.active = true
              and (:category is null or d.category.id = :category)
              and (:institution is null or d.institution.id = :institution)
              and (:faculty is null or d.faculty.id = :faculty)
              and (:access = '' or d.accessType = :access)
              and (lower(d.title) like lower(concat('%', :q, '%'))
                   or lower(d.summary) like lower(concat('%', :q, '%')))
            """)
    Page<Document> catalogue(@Param("q") String query,
            @Param("category") Long category,
            @Param("access") String access,
            @Param("institution") Long institution,
            @Param("faculty") Long faculty,
            Pageable page);

    @EntityGraph(attributePaths = { "category", "author", "institution", "faculty" })
    Optional<Document> findByIdAndStatus(Long id, String status);

    /**
     * Đếm số tài liệu của từng chủ đề.
     * Mỗi dòng kết quả là mảng: [id, tên, slug, số lượng].
     */
    @Query("""
            select d.category.id, d.category.name, d.category.slug, count(d)
            from Document d
            where d.status = 'PUBLISHED' and d.category.active = true
            group by d.category.id, d.category.name, d.category.slug
            order by d.category.name
            """)
    List<Object[]> categoryCounts();

    /**
     * Giống catalogue() nhưng sắp xếp theo số lượt tải kể từ thời điểm "since" (phổ biến nhất lên đầu).
     * countQuery dùng để đếm tổng số kết quả phục vụ phân trang.
     */
    @EntityGraph(attributePaths = { "category", "author", "institution", "faculty" })
    @Query(value = """
            select d from Document d
            where d.status = 'PUBLISHED'
              and d.category.active = true
              and (:category is null or d.category.id = :category)
              and (:institution is null or d.institution.id = :institution)
              and (:faculty is null or d.faculty.id = :faculty)
              and (:access = '' or d.accessType = :access)
              and (lower(d.title) like lower(concat('%', :q, '%'))
                   or lower(d.summary) like lower(concat('%', :q, '%')))
            order by (select count(dl) from DocumentDownload dl
                      where dl.document = d and dl.createdAt >= :since) desc,
                     d.id desc
            """, countQuery = """
            select count(d) from Document d
            where d.status = 'PUBLISHED'
              and d.category.active = true
              and (:category is null or d.category.id = :category)
              and (:institution is null or d.institution.id = :institution)
              and (:faculty is null or d.faculty.id = :faculty)
              and (:access = '' or d.accessType = :access)
              and (lower(d.title) like lower(concat('%', :q, '%'))
                   or lower(d.summary) like lower(concat('%', :q, '%')))
            """)
    Page<Document> popular(@Param("q") String q,
            @Param("category") Long category,
            @Param("access") String access,
            @Param("institution") Long institution,
            @Param("faculty") Long faculty,
            @Param("since") LocalDateTime since,
            Pageable page);

    /** Đếm tổng số tài liệu đã công bố. */
    @Query("select count(d) from Document d where d.status = 'PUBLISHED' and d.category.active = true")
    long countPublished();

    /** Đếm số tài liệu miễn phí đã công bố. */
    @Query("""
            select count(d) from Document d
            where d.status = 'PUBLISHED' and d.category.active = true and d.accessType = 'FREE'
            """)
    long countFree();

    /**
     * Ứng viên cho mục "Tài liệu liên quan": tài liệu đã công bố, khác tài liệu đang xem,
     * và cùng chủ đề HOẶC cùng môn học. Mới nhất lên đầu.
     *
     * Dùng "left join d.subject s" vì nhiều tài liệu không có môn học;
     * nếu viết thẳng d.subject.id thì JPA sẽ dùng inner join và loại mất các tài liệu đó.
     */
    @EntityGraph(attributePaths = { "category", "author", "institution", "faculty", "subject" })
    @Query("""
            select d from Document d
            left join d.subject s
            where d.status = 'PUBLISHED'
              and d.category.active = true
              and d.id <> :documentId
              and (d.category.id = :categoryId or s.id = :subjectId)
            order by d.publishedAt desc, d.id desc
            """)
    List<Document> relatedCandidates(@Param("documentId") Long documentId,
            @Param("categoryId") Long categoryId,
            @Param("subjectId") Long subjectId,
            Pageable page);

    /**
     * Tài liệu mới nhất (trừ tài liệu đang xem), dùng để bù cho đủ số lượng
     * khi không có đủ tài liệu cùng chủ đề / môn học.
     */
    @EntityGraph(attributePaths = { "category", "author", "institution", "faculty", "subject" })
    @Query("""
            select d from Document d
            where d.status = 'PUBLISHED'
              and d.category.active = true
              and d.id <> :documentId
            order by d.publishedAt desc, d.id desc
            """)
    List<Document> latestPublishedExcept(@Param("documentId") Long documentId, Pageable page);

    boolean existsByInstitutionId(Long institutionId);

    boolean existsByFacultyId(Long facultyId);
}
