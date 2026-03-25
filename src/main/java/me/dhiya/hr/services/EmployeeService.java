package me.dhiya.hr.services;

import me.dhiya.hr.domain.EmployeeEntity;
import me.dhiya.hr.domain.enums.Role;
import me.dhiya.hr.dto.employee.request.CreateEmployeeRequest;
import me.dhiya.hr.dto.employee.request.UpdateEmployeeRequest;
import me.dhiya.hr.dto.employee.response.EmployeePageDto;
import java.util.Optional;

public interface EmployeeService {
    Optional<EmployeeEntity> authenticate(String email, String password);
    EmployeeEntity setup(EmployeeEntity employee);
    EmployeeEntity getProfile(String employeeId);
    EmployeeEntity createEmployee(CreateEmployeeRequest request, EmployeeEntity createdBy);
    EmployeeEntity updateEmployee(String id, UpdateEmployeeRequest request, EmployeeEntity caller);
    EmployeeEntity deactivateEmployee(String id, EmployeeEntity caller);
    EmployeePageDto listEmployees(
            String cursor,
            int size,
            boolean includeInactive,
            String department,
            Role role,
            String search
    );
}
