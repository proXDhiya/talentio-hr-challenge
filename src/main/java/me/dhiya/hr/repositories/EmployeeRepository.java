package me.dhiya.hr.repositories;

import me.dhiya.hr.domain.EmployeeEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmployeeRepository extends CrudRepository<EmployeeEntity, String> {
    Optional<EmployeeEntity> findByEmail(String email);
}
