package me.dhiya.hr.dto.leave.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import me.dhiya.hr.domain.enums.LeaveStatus;
import me.dhiya.hr.domain.enums.LeaveType;
import me.dhiya.hr.dto.employee.response.EmployeeRefDto;
import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "LeaveRequestListItem")
public class LeaveRequestListItemDto {
    private String id;
    private EmployeeRefDto employee;
    private LocalDate startDate;
    private LocalDate endDate;
    private int totalDays;
    private LeaveType type;
    private LeaveStatus status;
    private String reason;
    private EmployeeRefDto reviewedBy;
    private String reviewComment;
    private Instant createdAt;
}
