package me.dhiya.hr.repositories;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import me.dhiya.hr.domain.EmployeeEntity;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends CrudRepository<EmployeeEntity, String> {
    Optional<EmployeeEntity> findByEmail(String email);

    @Query(nativeQuery = true, value = "SELECT e.* FROM employees e WHERE e.id = :id")
    Optional<EmployeeEntity> findByIdNative(@Param("id") String id);

    @Query(nativeQuery = true, value = """
            SELECT e.* FROM employees e
            WHERE (:includeInactive = TRUE OR e.status = 'ACTIVE')
              AND (:department IS NULL OR e.department = :department)
              AND (:role IS NULL OR e.role = :role)
              AND (:search IS NULL OR LOWER(e.first_name) LIKE :search
                   OR LOWER(e.last_name) LIKE :search
                   OR LOWER(e.email) LIKE :search)
              AND (:cursor IS NULL OR EXISTS (
                   SELECT 1 FROM employees ce
                   WHERE ce.id = :cursor
                   AND (e.created_at > ce.created_at
                        OR (e.created_at = ce.created_at AND e.id > ce.id))
              ))
            ORDER BY e.created_at ASC, e.id ASC
            LIMIT :limit
            """)
    List<EmployeeEntity> findEmployeesPage(
            @Param("includeInactive") boolean includeInactive,
            @Param("department") String department,
            @Param("role") String role,
            @Param("search") String search,
            @Param("cursor") String cursor,
            @Param("limit") int limit
    );
}
