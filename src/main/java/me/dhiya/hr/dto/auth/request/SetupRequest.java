package me.dhiya.hr.dto.auth.request;

import me.dhiya.hr.util.ValidationConstants;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SetupRequest {

    @NotBlank(message = "First name is required")
    private String firstName;

    @NotBlank(message = "Last name is required")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(regexp = ValidationConstants.EMAIL_REGEX, message = "Email must be a valid email address")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = ValidationConstants.PASSWORD_MIN, max = ValidationConstants.PASSWORD_MAX, message = "Password must be between 8 and 64 characters")
    @Pattern(regexp = ValidationConstants.PASSWORD_STRENGTH_REGEX, message = "Password must contain at least one uppercase letter and one number")
    private String password;

    @NotBlank(message = "Department is required")
    private String department;
}
