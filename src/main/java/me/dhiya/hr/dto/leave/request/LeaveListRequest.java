package me.dhiya.hr.dto.leave.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import me.dhiya.hr.domain.enums.EmployeeStatus;
import me.dhiya.hr.domain.enums.LeaveStatus;
import me.dhiya.hr.domain.enums.LeaveType;
import me.dhiya.hr.util.ValidationConstants;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;

@Data
public class LeaveListRequest {

    @Min(value = 1, message = "Size must be at least 1")
    @Max(value = 100, message = "Size must not exceed 100")
    private int size = 20;

    @Pattern(regexp = ValidationConstants.UUID_REGEX, message = "Cursor must be a valid UUID")
    private String cursor;

    @Pattern(regexp = ValidationConstants.UUID_REGEX, message = "Employee ID must be a valid UUID")
    private String employeeId;

    private EmployeeStatus employeeStatus = EmployeeStatus.ACTIVE;

    private LeaveStatus status;

    private LeaveType type;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fromDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate toDate;
}
