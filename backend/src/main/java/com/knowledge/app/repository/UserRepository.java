package com.knowledge.app.repository;
import com.knowledge.app.model.entity.User;
import java.util.Optional;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
public interface UserRepository extends JpaRepository<User, Long> {
 Optional<User> findByUsernameIgnoreCase(String username);
 Optional<User> findByEmailIgnoreCase(String email);
 boolean existsByUsernameIgnoreCase(String username);
 boolean existsByEmailIgnoreCase(String email);
 boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);
 @Query("select u from User u where lower(u.username) like lower(concat('%',:q,'%')) or lower(u.email) like lower(concat('%',:q,'%'))")
 Page<User> search(@Param("q") String query, Pageable pageable);
 @Query("select u from User u where u.status='ACTIVE' and (lower(u.username) like lower(concat('%',:q,'%')) or exists(select p.id from UserProfile p where p.user=u and lower(p.displayName) like lower(concat('%',:q,'%'))))")
 Page<User> community(@Param("q") String q,Pageable page);
}
