package me.dhiya.hr.dto.employee.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "EmployeeListItem")
public class EmployeeListItemDto {
    private String id;
    private String firstName;
    private String lastName;
    private String email;
    private String role;
    private String department;
    private String position;
    private String status;
    private LocalDate hireDate;
    private ManagerDto manager;
}
