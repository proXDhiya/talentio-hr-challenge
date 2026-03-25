package me.dhiya.hr.dto.employee.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import me.dhiya.hr.domain.enums.Role;
import me.dhiya.hr.util.ValidationConstants;

@Data
public class EmployeeListRequest {

    @Min(value = 1, message = "Size must be at least 1")
    @Max(value = 100, message = "Size must not exceed 100")
    private int size = 20;

    @Pattern(regexp = ValidationConstants.UUID_REGEX, message = "Cursor must be a valid UUID")
    private String cursor;

    private boolean includeInactive = false;

    @Size(min = 1, max = 100, message = "Department must be between 1 and 100 characters")
    private String department;

    private Role role;

    @Size(min = 1, max = 100, message = "Search must be between 1 and 100 characters")
    private String search;
}
