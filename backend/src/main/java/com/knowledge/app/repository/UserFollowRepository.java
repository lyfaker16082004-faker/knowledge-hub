package com.knowledge.app.repository;

import com.knowledge.app.model.entity.UserFollow;
import java.util.Optional;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface UserFollowRepository extends JpaRepository<UserFollow, Long> {
    Optional<UserFollow> findByFollowerIdAndFollowingId(Long follower, Long following);

    boolean existsByFollowerIdAndFollowingId(Long follower, Long following);

    @Query("select count(f) from UserFollow f where f.following.id=:id and f.follower.status='ACTIVE'")
    long followers(@Param("id") Long id);

    @Query("select count(f) from UserFollow f where f.follower.id=:id and f.following.status='ACTIVE'")
    long following(@Param("id") Long id);

    @EntityGraph(attributePaths = { "following" })
    @Query("select f from UserFollow f where f.follower.id=:id and f.following.status='ACTIVE'")
    Page<UserFollow> followingPage(@Param("id") Long id, Pageable page);

    @EntityGraph(attributePaths = { "follower" })
    @Query("select f from UserFollow f where f.following.id=:id and f.follower.status='ACTIVE'")
    Page<UserFollow> followersPage(@Param("id") Long id, Pageable page);
}
