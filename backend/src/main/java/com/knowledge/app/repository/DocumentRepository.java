package com.knowledge.app.repository;

import com.knowledge.app.model.entity.Document;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface DocumentRepository extends JpaRepository<Document, Long>, JpaSpecificationExecutor<Document> {
    @EntityGraph(attributePaths = { "category", "author", "institution", "faculty" })
    @Query("select d from Document d where d.status='PUBLISHED' and d.category.active=true and (:category is null or d.category.id=:category) and (:institution is null or d.institution.id=:institution) and (:faculty is null or d.faculty.id=:faculty) and (:access='' or d.accessType=:access) and (lower(d.title) like lower(concat('%',:q,'%')) or lower(d.summary) like lower(concat('%',:q,'%')))")
    Page<Document> catalogue(@Param("q") String query, @Param("category") Long category, @Param("access") String access,
            @Param("institution") Long institution, @Param("faculty") Long faculty, Pageable page);

    @EntityGraph(attributePaths = { "category", "author", "institution", "faculty" })
    Optional<Document> findByIdAndStatus(Long id, String status);

    @Query("select d.category.id, d.category.name, d.category.slug, count(d) from Document d where d.status='PUBLISHED' and d.category.active=true group by d.category.id,d.category.name,d.category.slug order by d.category.name")
    List<Object[]> categoryCounts();

    @EntityGraph(attributePaths = { "category", "author", "institution", "faculty" })
    @Query(value = "select d from Document d where d.status='PUBLISHED' and d.category.active=true and (:category is null or d.category.id=:category) and (:institution is null or d.institution.id=:institution) and (:faculty is null or d.faculty.id=:faculty) and (:access='' or d.accessType=:access) and (lower(d.title) like lower(concat('%',:q,'%')) or lower(d.summary) like lower(concat('%',:q,'%'))) order by (select count(dl) from DocumentDownload dl where dl.document=d and dl.createdAt>=:since) desc,d.id desc", countQuery = "select count(d) from Document d where d.status='PUBLISHED' and d.category.active=true and (:category is null or d.category.id=:category) and (:institution is null or d.institution.id=:institution) and (:faculty is null or d.faculty.id=:faculty) and (:access='' or d.accessType=:access) and (lower(d.title) like lower(concat('%',:q,'%')) or lower(d.summary) like lower(concat('%',:q,'%')))")
    Page<Document> popular(@Param("q") String q, @Param("category") Long category, @Param("access") String access,
            @Param("institution") Long institution, @Param("faculty") Long faculty,
            @Param("since") java.time.LocalDateTime since, Pageable page);

    @Query("select count(d) from Document d where d.status='PUBLISHED' and d.category.active=true")
    long countPublished();

    @Query("select count(d) from Document d where d.status='PUBLISHED' and d.category.active=true and d.accessType='FREE'")
    long countFree();

    boolean existsByInstitutionId(Long institutionId);

    boolean existsByFacultyId(Long facultyId);

}
