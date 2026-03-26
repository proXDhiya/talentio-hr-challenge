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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import me.dhiya.hr.domain.EmployeeEntity;
import me.dhiya.hr.domain.LeaveRequestEntity;
import me.dhiya.hr.dto.leave.request.ApproveLeaveRequest;
import me.dhiya.hr.dto.leave.request.CreateLeaveRequest;
import me.dhiya.hr.dto.leave.request.LeaveListRequest;
import me.dhiya.hr.dto.leave.request.RejectLeaveRequest;
import me.dhiya.hr.dto.leave.response.LeaveRequestDto;
import me.dhiya.hr.dto.leave.response.LeaveRequestListResponse;
import me.dhiya.hr.dto.leave.response.LeaveRequestPageDto;
import me.dhiya.hr.dto.leave.response.LeaveRequestResponse;
import me.dhiya.hr.dto.leave.response.LeaveReviewDto;
import me.dhiya.hr.dto.leave.response.LeaveReviewResponse;
import me.dhiya.hr.services.LeaveRequestService;
import me.dhiya.hr.util.ApiExamples;
import java.time.Instant;

@RestController
@RequestMapping("/apis/v1/leave-requests")
@Tag(name = "Leave Requests", description = "Submit, review, and manage employee leave requests. All authenticated users can submit and list requests. Approve/reject require MANAGER or HR role.")
public class LeaveRequestController {

    private final LeaveRequestService leaveRequestService;

    public LeaveRequestController(LeaveRequestService leaveRequestService) {
        this.leaveRequestService = leaveRequestService;
    }

    @Operation(
            summary = "List leave requests",
            description = "Returns a cursor-paginated list of leave requests. Page size defaults to `20`, max `100`.\n\n" +
                    "**Filters:**\n" +
                    "* `employeeId` - filter by a specific employee (UUID)\n" +
                    "* `status` - `PENDING`, `APPROVED`, or `REJECTED`\n" +
                    "* `type` - `ANNUAL`, `SICK`, or `UNPAID`\n" +
                    "* `fromDate` - leaves whose start date is on or after this date (ISO format: `yyyy-MM-dd`)\n" +
                    "* `toDate` - leaves whose end date is on or before this date (ISO format: `yyyy-MM-dd`)\n" +
                    "* `employeeStatus` - `ACTIVE` or `INACTIVE` (default: `ACTIVE`)\n\n" +
                    "**Access:** all authenticated users"
    )
    @SecurityRequirement(name = "Bearer")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Leave requests retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = LeaveRequestListResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = ApiExamples.VALIDATION_ERROR))),
            @ApiResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = ApiExamples.UNAUTHORIZED_PROFILE)))
    })
    @GetMapping(produces = "application/json")
    public ResponseEntity<LeaveRequestListResponse> listLeaveRequests(
            @ModelAttribute @Valid LeaveListRequest request) {
        LeaveRequestPageDto page = leaveRequestService.listLeaveRequests(
                request.getCursor(), request.getSize(), request.getEmployeeId(),
                request.getEmployeeStatus(), request.getStatus(), request.getType(),
                request.getFromDate(), request.getToDate()
        );

        return ResponseEntity.ok(LeaveRequestListResponse.builder()
                .message("Leave requests retrieved successfully")
                .data(page)
                .timestamp(Instant.now())
                .build());
    }

    @Operation(
            summary = "Get leave request by ID",
            description = "Returns a single leave request with full details including employee info, reviewer, and review comment.\n\n" +
                    "**Access rules:**\n" +
                    "* **EMPLOYEE** can only view their own leave requests - returns `403` for any other employee's request\n" +
                    "* **MANAGER** and **HR** can view any leave request"
    )
    @SecurityRequirement(name = "Bearer")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Leave request retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = LeaveRequestResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = ApiExamples.UNAUTHORIZED_PROFILE))),
            @ApiResponse(responseCode = "403", description = "Access denied",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = ApiExamples.FORBIDDEN))),
            @ApiResponse(responseCode = "404", description = "Leave request not found",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = ApiExamples.NOT_FOUND)))
    })
    @GetMapping(value = "/{id}", produces = "application/json")
    public ResponseEntity<LeaveRequestResponse> getLeaveRequestById(
            @PathVariable String id,
            @AuthenticationPrincipal EmployeeEntity currentUser) {
        LeaveRequestDto dto = leaveRequestService.getLeaveRequestById(id, currentUser);

        return ResponseEntity.ok(LeaveRequestResponse.builder()
                .message("Leave request retrieved successfully")
                .data(dto)
                .timestamp(Instant.now())
                .build());
    }

    @Operation(
            summary = "Submit leave request",
            description = "Submits a new leave request for the authenticated employee.\n\n" +
                    "**Leave types:**\n" +
                    "* `ANNUAL` - deducted from the employee's annual leave balance\n" +
                    "* `SICK` - no balance check\n" +
                    "* `UNPAID` - no balance check\n\n" +
                    "**Validation:**\n" +
                    "* `startDate` must be today or a future date\n" +
                    "* `endDate` must be on or after `startDate`\n" +
                    "* `reason` is optional, max 500 characters\n" +
                    "* Returns `409` if dates overlap with an existing `PENDING` or `APPROVED` leave\n" +
                    "* Returns `422` if type is `ANNUAL` and the remaining balance is insufficient"
    )
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
                .data(leaveRequestService.getLeaveRequestById(entity.getId(), currentUser))
                .timestamp(Instant.now())
                .build());
    }

    @Operation(
            summary = "Approve leave request",
            description = "Approves a `PENDING` leave request.\n\n" +
                    "**Access rules:**\n" +
                    "* **HR** can approve any leave request\n" +
                    "* **MANAGER** can only approve requests from employees they directly manage - returns `403` otherwise\n\n" +
                    "**Notes:**\n" +
                    "* `comment` is optional, max 500 characters\n" +
                    "* Returns `422` if the leave is not in `PENDING` status"
    )
    @SecurityRequirement(name = "Bearer")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Leave request approved successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = LeaveReviewResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = ApiExamples.UNAUTHORIZED_PROFILE))),
            @ApiResponse(responseCode = "403", description = "Access denied",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = ApiExamples.FORBIDDEN))),
            @ApiResponse(responseCode = "404", description = "Leave request not found",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = ApiExamples.NOT_FOUND))),
            @ApiResponse(responseCode = "422", description = "Leave request is not in PENDING state",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = ApiExamples.LEAVE_INVALID_STATE)))
    })
    @PatchMapping(value = "/{id}/approve", produces = "application/json")
    public ResponseEntity<LeaveReviewResponse> approveLeaveRequest(
            @PathVariable String id,
            @RequestBody(required = false) ApproveLeaveRequest request,
            @AuthenticationPrincipal EmployeeEntity currentUser) {
        LeaveReviewDto dto = leaveRequestService.approveLeaveRequest(
                id, request != null ? request.getComment() : null, currentUser);

        return ResponseEntity.ok(LeaveReviewResponse.builder()
                .message("Leave request approved successfully")
                .data(dto)
                .timestamp(Instant.now())
                .build());
    }

    @Operation(
            summary = "Reject leave request",
            description = "Rejects a `PENDING` leave request.\n\n" +
                    "**Access rules:**\n" +
                    "* **HR** can reject any leave request\n" +
                    "* **MANAGER** can only reject requests from employees they directly manage - returns `403` otherwise\n\n" +
                    "**Notes:**\n" +
                    "* `comment` is **required**, max 500 characters - returns `400` if missing or blank\n" +
                    "* Returns `422` if the leave is not in `PENDING` status"
    )
    @SecurityRequirement(name = "Bearer")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Leave request rejected",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = LeaveReviewResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = ApiExamples.VALIDATION_ERROR))),
            @ApiResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = ApiExamples.UNAUTHORIZED_PROFILE))),
            @ApiResponse(responseCode = "403", description = "Access denied",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = ApiExamples.FORBIDDEN))),
            @ApiResponse(responseCode = "404", description = "Leave request not found",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = ApiExamples.NOT_FOUND))),
            @ApiResponse(responseCode = "422", description = "Leave request is not in PENDING state",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = ApiExamples.LEAVE_INVALID_STATE)))
    })
    @PatchMapping(value = "/{id}/reject", produces = "application/json")
    public ResponseEntity<LeaveReviewResponse> rejectLeaveRequest(
            @PathVariable String id,
            @Valid @RequestBody RejectLeaveRequest request,
            @AuthenticationPrincipal EmployeeEntity currentUser) {
        LeaveReviewDto dto = leaveRequestService.rejectLeaveRequest(id, request.getComment(), currentUser);

        return ResponseEntity.ok(LeaveReviewResponse.builder()
                .message("Leave request rejected")
                .data(dto)
                .timestamp(Instant.now())
                .build());
    }

}
