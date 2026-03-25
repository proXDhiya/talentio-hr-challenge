package me.dhiya.hr.repositories;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import me.dhiya.hr.domain.EmployeeEntity;

import java.util.Optional;

@Repository
public interface EmployeeRepository extends CrudRepository<EmployeeEntity, String> {
    Optional<EmployeeEntity> findByEmail(String email);
}
