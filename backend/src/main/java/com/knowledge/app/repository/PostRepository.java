package com.knowledge.app.repository;

import com.knowledge.app.model.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long> {
    boolean existsByInstitutionId(Long institutionId);

    boolean existsByFacultyId(Long facultyId);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = { "author" })
    @org.springframework.data.jpa.repository.Query("select p from Post p where p.status='PUBLISHED' and p.author.status='ACTIVE' and p.visibility in ('PUBLIC','FOLLOWERS') and exists(select f.id from UserFollow f where f.following=p.author and f.follower.id=:viewer)")
    org.springframework.data.domain.Page<Post> followedPosts(
            @org.springframework.data.repository.query.Param("viewer") Long viewer,
            org.springframework.data.domain.Pageable page);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = { "author" })
    @org.springframework.data.jpa.repository.Query("select p from Post p where p.author.id=:author and p.author.status='ACTIVE' and p.status='PUBLISHED' and (p.visibility='PUBLIC' or (p.visibility='FOLLOWERS' and :follows=true))")
    org.springframework.data.domain.Page<Post> authorPosts(
            @org.springframework.data.repository.query.Param("author") Long author,
            @org.springframework.data.repository.query.Param("follows") boolean follows,
            org.springframework.data.domain.Pageable page);
}
