package me.dhiya.hr.dto.employee.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import me.dhiya.hr.domain.enums.Role;
import me.dhiya.hr.util.ValidationConstants;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateEmployeeRequest {

    @Size(min = 1, max = 50, message = "First name must be between 1 and 50 characters")
    private String firstName;

    @Size(min = 1, max = 50, message = "Last name must be between 1 and 50 characters")
    private String lastName;

    @Size(min = 1, max = 100, message = "Department must be between 1 and 100 characters")
    private String department;

    @Size(min = 1, max = 100, message = "Position must be between 1 and 100 characters")
    private String position;

    @DecimalMin(value = "0.0", inclusive = false, message = "Salary must be a positive number")
    private BigDecimal salary;

    @Size(min = 3, max = 3, message = "Currency code must be exactly 3 characters")
    private String currencyCode;

    @Min(value = 1, message = "Annual leave days must be at least 1")
    private Integer annualLeaveDays;

    @Pattern(regexp = ValidationConstants.UUID_REGEX, message = "Manager ID must be a valid UUID")
    private String managerId;

    private Role role;
}
