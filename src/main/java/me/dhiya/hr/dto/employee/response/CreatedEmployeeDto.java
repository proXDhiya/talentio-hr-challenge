package me.dhiya.hr.dto.employee.response;

import me.dhiya.hr.domain.enums.EmployeeStatus;
import me.dhiya.hr.domain.enums.Role;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(name = "CreatedEmployee")
public class CreatedEmployeeDto {
    private String id;
    private String firstName;
    private String lastName;
    private String email;
    private Role role;
    private String department;
    private String position;
    private BigDecimal salary;
    private CurrencyDto currency;
    private ManagerDto manager;
    private LocalDate hireDate;
    private Integer annualLeaveDays;
    private EmployeeStatus status;
    private Instant createdAt;
}
