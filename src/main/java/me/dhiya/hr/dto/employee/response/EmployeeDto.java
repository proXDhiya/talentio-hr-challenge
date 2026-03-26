package me.dhiya.hr.dto.employee.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import me.dhiya.hr.domain.enums.EmployeeStatus;
import me.dhiya.hr.domain.enums.Role;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "Employee")
public class EmployeeDto {
    private String id;
    private String firstName;
    private String lastName;
    private String email;
    private Role role;
    private String department;
    private String position;
    private BigDecimal salary;
    private CurrencyDto currency;
    private EmployeeRefDto manager;
    private LocalDate hireDate;
    private Integer annualLeaveDays;
    private Integer usedLeaveDays;
    private Integer remainingLeaveDays;
    private EmployeeStatus status;
    private Instant createdAt;
    private Instant deletedAt;
}
