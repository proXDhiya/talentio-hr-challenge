package me.dhiya.hr.dto.response;

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
@Schema(name = "EmployeeProfile")
public class EmployeeProfileDto {
    private String id;
    private String firstName;
    private String lastName;
    private String email;
    private Role role;
    private String department;
    private String position;
    private BigDecimal salary;
    private CurrencyDto currency;
    private LocalDate hireDate;
    private Integer annualLeaveDays;
    private Integer usedLeaveDays;
    private Integer remainingLeaveDays;
    private EmployeeStatus status;
    private Instant createdAt;
}
