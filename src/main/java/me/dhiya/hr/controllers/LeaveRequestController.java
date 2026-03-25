package me.dhiya.hr.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import me.dhiya.hr.domain.EmployeeEntity;
import me.dhiya.hr.domain.LeaveRequestEntity;
import me.dhiya.hr.dto.employee.response.ManagerDto;
import me.dhiya.hr.dto.leave.request.CreateLeaveRequest;
import me.dhiya.hr.dto.leave.response.LeaveRequestDto;
import me.dhiya.hr.dto.leave.response.LeaveRequestResponse;
import me.dhiya.hr.services.LeaveRequestService;
import me.dhiya.hr.util.ApiExamples;
import java.time.Instant;

@RestController
@RequestMapping("/apis/v1/leave-requests")
@Tag(name = "Leave Requests", description = "Leave request management endpoints")
public class LeaveRequestController {

    private final LeaveRequestService leaveRequestService;

    public LeaveRequestController(LeaveRequestService leaveRequestService) {
        this.leaveRequestService = leaveRequestService;
    }

    @Operation(summary = "Submit leave request", description = "Submits a new leave request for the authenticated employee.")
    @SecurityRequirement(name = "Bearer")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Leave request submitted successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = LeaveRequestResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = ApiExamples.VALIDATION_ERROR))),
            @ApiResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = ApiExamples.UNAUTHORIZED_PROFILE))),
            @ApiResponse(responseCode = "409", description = "Overlapping leave request exists",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = ApiExamples.LEAVE_OVERLAP))),
            @ApiResponse(responseCode = "422", description = "Insufficient leave balance",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = ApiExamples.LEAVE_INSUFFICIENT_BALANCE)))
    })
    @PostMapping(produces = "application/json")
    public ResponseEntity<LeaveRequestResponse> submitLeaveRequest(
            @Valid @RequestBody CreateLeaveRequest request,
            @AuthenticationPrincipal EmployeeEntity currentUser
    ) {
        LeaveRequestEntity entity = leaveRequestService.submitLeaveRequest(request, currentUser);

        return ResponseEntity.status(HttpStatus.CREATED).body(LeaveRequestResponse.builder()
                .message("Leave request submitted successfully")
                .data(toDto(entity))
                .timestamp(Instant.now())
                .build());
    }

    private LeaveRequestDto toDto(LeaveRequestEntity entity) {
        int totalDays = (int) (entity.getEndDate().toEpochDay() - entity.getStartDate().toEpochDay() + 1);

        return LeaveRequestDto.builder()
                .id(entity.getId())
                .employee(ManagerDto.builder()
                        .id(entity.getEmployee().getId())
                        .firstName(entity.getEmployee().getFirstName())
                        .lastName(entity.getEmployee().getLastName())
                        .build())
                .startDate(entity.getStartDate())
                .endDate(entity.getEndDate())
                .totalDays(totalDays)
                .type(entity.getType())
                .status(entity.getStatus())
                .reason(entity.getReason())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
