package me.dhiya.hr.dto.leave.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ApproveLeaveRequest {
    @Size(max = 500, message = "Comment must not exceed 500 characters")
    private String comment;
}
