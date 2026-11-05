package com.knowledge.app.repository;
import com.knowledge.app.model.entity.Role;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
public interface RoleRepository extends JpaRepository<Role, Long> {
 Optional<Role> findByName(String name);
 @Lock(LockModeType.PESSIMISTIC_WRITE)
 @Query("select r from Role r where r.name=:name")
 Optional<Role> lockByName(@Param("name") String name);
}
