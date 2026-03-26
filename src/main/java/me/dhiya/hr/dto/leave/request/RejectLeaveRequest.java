package me.dhiya.hr.dto.leave.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RejectLeaveRequest {

    @NotBlank(message = "A reason is required when rejecting a leave request")
    @Size(max = 500, message = "Comment must not exceed 500 characters")
    private String comment;
}
