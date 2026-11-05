package com.knowledge.app.repository;
import com.knowledge.app.model.entity.UserRole;
import java.util.List;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
public interface UserRoleRepository extends JpaRepository<UserRole, Long> {
 @Query("select ur from UserRole ur join fetch ur.role where ur.user.id=:id")
 List<UserRole> findWithRoles(@Param("id") Long id);
 @Query("select count(ur) from UserRole ur where ur.role.name='ADMIN' and ur.user.status='ACTIVE'")
 long countActiveAdmins();
 @Modifying @Query("delete from UserRole ur where ur.user.id=:id")
 void removeForUser(@Param("id") Long id);
}
