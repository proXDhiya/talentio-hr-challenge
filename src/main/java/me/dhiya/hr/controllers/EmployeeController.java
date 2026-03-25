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
import org.springframework.web.bind.annotation.*;
import me.dhiya.hr.domain.EmployeeEntity;
import me.dhiya.hr.dto.employee.request.CreateEmployeeRequest;
import me.dhiya.hr.dto.employee.request.EmployeeListRequest;
import me.dhiya.hr.dto.employee.request.UpdateEmployeeRequest;
import me.dhiya.hr.dto.employee.response.CurrencyDto;
import me.dhiya.hr.dto.employee.response.EmployeeDto;
import me.dhiya.hr.dto.employee.response.EmployeeListResponse;
import me.dhiya.hr.dto.employee.response.EmployeePageDto;
import me.dhiya.hr.dto.employee.response.EmployeeResponse;
import me.dhiya.hr.dto.employee.response.ManagerDto;
import me.dhiya.hr.services.EmployeeService;
import me.dhiya.hr.services.LeaveRequestService;
import me.dhiya.hr.util.ApiExamples;
import java.time.Instant;

@RestController
@RequestMapping("/apis/v1/employees")
@Tag(name = "Employees", description = "Employee management endpoints")
public class EmployeeController {

    private final EmployeeService employeeService;
    private final LeaveRequestService leaveRequestService;

    public EmployeeController(EmployeeService employeeService, LeaveRequestService leaveRequestService) {
        this.employeeService = employeeService;
        this.leaveRequestService = leaveRequestService;
    }

    @Operation(summary = "List employees", description = "Returns paginated employee list with optional filters. HR and Manager only.")
    @SecurityRequirement(name = "Bearer")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Employees retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = EmployeeListResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = ApiExamples.VALIDATION_ERROR))),
            @ApiResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = ApiExamples.UNAUTHORIZED_PROFILE))),
            @ApiResponse(responseCode = "403", description = "Access denied",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = ApiExamples.FORBIDDEN)))
    })
    @GetMapping(produces = "application/json")
    public ResponseEntity<EmployeeListResponse> listEmployees(@ModelAttribute @Valid EmployeeListRequest request) {
        EmployeePageDto page = employeeService.listEmployees(
                request.getCursor(), request.getSize(), request.isIncludeInactive(),
                request.getDepartment(), request.getRole(), request.getSearch()
        );

        return ResponseEntity.ok(EmployeeListResponse.builder()
                .message("Employees retrieved successfully")
                .data(page)
                .timestamp(Instant.now())
                .build());
    }

    @Operation(summary = "Get employee by ID", description = "Returns a single employee's profile and leave balance. HR and Manager only.")
    @SecurityRequirement(name = "Bearer")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Employee retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = EmployeeResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = ApiExamples.UNAUTHORIZED_PROFILE))),
            @ApiResponse(responseCode = "403", description = "Access denied",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = ApiExamples.FORBIDDEN))),
            @ApiResponse(responseCode = "404", description = "Employee not found",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = ApiExamples.NOT_FOUND)))
    })
    @GetMapping(value = "/{id}", produces = "application/json")
    public ResponseEntity<EmployeeResponse> getEmployeeById(@PathVariable String id) {
        EmployeeEntity employee = employeeService.getProfile(id);
        int usedDays = leaveRequestService.calculateUsedLeaveDays(employee);

        return ResponseEntity.ok(EmployeeResponse.builder()
                .message("Employee retrieved successfully")
                .data(toDto(employee, usedDays))
                .timestamp(Instant.now())
                .build());
    }

    @Operation(summary = "Get my profile", description = "Returns the authenticated employee's profile and leave balance")
    @SecurityRequirement(name = "Bearer")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = EmployeeResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = ApiExamples.UNAUTHORIZED_PROFILE)))
    })
    @GetMapping(value = "/me", produces = "application/json")
    public ResponseEntity<EmployeeResponse> getProfile(@AuthenticationPrincipal EmployeeEntity employee) {
        int usedDays = leaveRequestService.calculateUsedLeaveDays(employee);

        return ResponseEntity.ok(EmployeeResponse.builder()
                .message("Profile retrieved successfully")
                .data(toDto(employee, usedDays))
                .timestamp(Instant.now())
                .build());
    }

    @Operation(summary = "Deactivate employee", description = "Soft deletes an employee by setting status to INACTIVE. HR can only deactivate employees, Manager can deactivate anyone.")
    @SecurityRequirement(name = "Bearer")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Employee deactivated successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = EmployeeResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = ApiExamples.UNAUTHORIZED_PROFILE))),
            @ApiResponse(responseCode = "403", description = "Access denied",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = ApiExamples.FORBIDDEN))),
            @ApiResponse(responseCode = "404", description = "Employee not found",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = ApiExamples.NOT_FOUND))),
            @ApiResponse(responseCode = "409", description = "Employee already deactivated",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = ApiExamples.ALREADY_INACTIVE)))
    })
    @DeleteMapping(value = "/{id}", produces = "application/json")
    public ResponseEntity<EmployeeResponse> deactivateEmployee(
            @PathVariable String id,
            @AuthenticationPrincipal EmployeeEntity currentUser
    ) {
        EmployeeEntity employee = employeeService.deactivateEmployee(id, currentUser);

        return ResponseEntity.ok(EmployeeResponse.builder()
                .message("Employee deactivated successfully")
                .data(EmployeeDto.builder()
                        .id(employee.getId())
                        .status(employee.getStatus())
                        .deletedAt(employee.getDeletedAt())
                        .build())
                .timestamp(Instant.now())
                .build());
    }

    @Operation(summary = "Update employee", description = "Updates employee information. HR can only update employees, Manager can update anyone.")
    @SecurityRequirement(name = "Bearer")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Employee updated successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = EmployeeResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = ApiExamples.VALIDATION_ERROR))),
            @ApiResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = ApiExamples.UNAUTHORIZED_PROFILE))),
            @ApiResponse(responseCode = "403", description = "Access denied",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = ApiExamples.FORBIDDEN))),
            @ApiResponse(responseCode = "404", description = "Employee not found",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = ApiExamples.NOT_FOUND)))
    })
    @PutMapping(value = "/{id}", produces = "application/json")
    public ResponseEntity<EmployeeResponse> updateEmployee(
            @PathVariable String id,
            @Valid @RequestBody UpdateEmployeeRequest request,
            @AuthenticationPrincipal EmployeeEntity currentUser
    ) {
        EmployeeEntity employee = employeeService.updateEmployee(id, request, currentUser);

        return ResponseEntity.ok(EmployeeResponse.builder()
                .message("Employee updated successfully")
                .data(toDto(employee, null))
                .timestamp(Instant.now())
                .build());
    }

    @Operation(summary = "Create employee", description = "Creates a new employee. HR and Manager only.")
    @SecurityRequirement(name = "Bearer")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Employee created successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = EmployeeResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = ApiExamples.VALIDATION_ERROR))),
            @ApiResponse(responseCode = "401", description = "Authentication required",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = ApiExamples.UNAUTHORIZED_PROFILE))),
            @ApiResponse(responseCode = "403", description = "Access denied",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = ApiExamples.FORBIDDEN))),
            @ApiResponse(responseCode = "409", description = "Email already exists",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = ApiExamples.EMAIL_CONFLICT)))
    })
    @PostMapping(produces = "application/json")
    public ResponseEntity<EmployeeResponse> createEmployee(
            @Valid @RequestBody CreateEmployeeRequest request,
            @AuthenticationPrincipal EmployeeEntity currentUser
    ) {
        EmployeeEntity employee = employeeService.createEmployee(request, currentUser);

        return ResponseEntity.status(HttpStatus.CREATED).body(EmployeeResponse.builder()
                .message("Employee created successfully")
                .data(toDto(employee, null))
                .timestamp(Instant.now())
                .build());
    }

    private EmployeeDto toDto(EmployeeEntity e, Integer usedDays) {
        return EmployeeDto.builder()
                .id(e.getId())
                .firstName(e.getFirstName())
                .lastName(e.getLastName())
                .email(e.getEmail())
                .role(e.getRole())
                .department(e.getDepartment())
                .position(e.getPosition())
                .salary(e.getSalary())
                .currency(e.getCurrency() != null ? CurrencyDto.builder()
                        .code(e.getCurrency().getCode())
                        .symbol(e.getCurrency().getSymbol())
                        .build() : null)
                .manager(e.getManager() != null ? ManagerDto.builder()
                        .id(e.getManager().getId())
                        .firstName(e.getManager().getFirstName())
                        .lastName(e.getManager().getLastName())
                        .build() : null)
                .hireDate(e.getHireDate())
                .annualLeaveDays(e.getAnnualLeaveDays())
                .usedLeaveDays(usedDays)
                .remainingLeaveDays(usedDays != null ? e.getAnnualLeaveDays() - usedDays : null)
                .status(e.getStatus())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
