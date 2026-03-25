package me.dhiya.hr.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import me.dhiya.hr.domain.enums.Role;

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
