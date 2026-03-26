package me.dhiya.hr.dto.employee.request;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import me.dhiya.hr.domain.enums.Role;
import me.dhiya.hr.util.ValidationConstants;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateEmployeeRequest {

    @NotBlank(message = "First name is required")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(regexp = ValidationConstants.EMAIL_REGEX, message = "Email must be a valid email address")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = ValidationConstants.PASSWORD_MIN, max = ValidationConstants.PASSWORD_MAX,
            message = "Password must be between 8 and 64 characters")
    @Pattern(regexp = ValidationConstants.PASSWORD_STRENGTH_REGEX,
            message = "Password must contain at least one uppercase letter and one number")
    private String password;

    @NotNull(message = "Role is required")
    private Role role;

    @NotBlank(message = "Department is required")
    private String department;

    @NotBlank(message = "Position is required")
    private String position;

    @NotNull(message = "Salary is required")
    @Positive(message = "Salary must be a positive number")
    private BigDecimal salary;

    @NotBlank(message = "Currency code is required")
    private String currencyCode;

    @NotNull(message = "Hire date is required")
    @PastOrPresent(message = "Hire date cannot be a future date")
    private LocalDate hireDate;

    @Min(value = 0, message = "Annual leave days cannot be negative")
    @Max(value = 365, message = "Annual leave days cannot exceed 365")
    private Integer annualLeaveDays;
}
