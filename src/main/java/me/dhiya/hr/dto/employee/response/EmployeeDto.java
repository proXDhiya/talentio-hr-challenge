package me.dhiya.hr.dto.employee.response;

import me.dhiya.hr.domain.enums.Role;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "Employee")
public class EmployeeDto {
    private String id;
    private String firstName;
    private String lastName;
    private String email;
    private Role role;
    private String department;
}
