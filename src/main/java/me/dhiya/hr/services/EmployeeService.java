package me.dhiya.hr.services;

import me.dhiya.hr.domain.EmployeeEntity;

import java.util.Optional;

public interface EmployeeService {
    Optional<EmployeeEntity> authenticate(String email, String password);
    EmployeeEntity setup(EmployeeEntity employee);
    EmployeeEntity getProfile(String employeeId);
    int calculateUsedLeaveDays(EmployeeEntity employee);
}
