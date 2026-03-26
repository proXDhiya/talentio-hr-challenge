package me.dhiya.hr.dto.leave.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import me.dhiya.hr.domain.enums.LeaveStatus;
import me.dhiya.hr.dto.employee.response.EmployeeRefDto;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "LeaveReview")
public class LeaveReviewDto {
    private String id;
    private LeaveStatus status;
    private EmployeeRefDto reviewedBy;
    private String reviewComment;
    private Instant updatedAt;
}
