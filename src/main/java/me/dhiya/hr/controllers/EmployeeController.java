package me.dhiya.hr.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import me.dhiya.hr.domain.EmployeeEntity;
import me.dhiya.hr.dto.response.CurrencyDto;
import me.dhiya.hr.dto.response.EmployeeProfileDto;
import me.dhiya.hr.dto.response.EmployeeResponse;
import me.dhiya.hr.services.EmployeeService;
import me.dhiya.hr.util.ApiExamples;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/apis/v1/employees")
@Tag(name = "Employees", description = "Employee management endpoints")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
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
        int usedDays = employeeService.calculateUsedLeaveDays(employee);

        EmployeeProfileDto profile = EmployeeProfileDto.builder()
                .id(employee.getId())
                .firstName(employee.getFirstName())
                .lastName(employee.getLastName())
                .email(employee.getEmail())
                .role(employee.getRole())
                .department(employee.getDepartment())
                .position(employee.getPosition())
                .salary(employee.getSalary())
                .currency(employee.getCurrency() != null ? CurrencyDto.builder()
                        .code(employee.getCurrency().getCode())
                        .symbol(employee.getCurrency().getSymbol())
                        .build() : null)
                .hireDate(employee.getHireDate())
                .annualLeaveDays(employee.getAnnualLeaveDays())
                .usedLeaveDays(usedDays)
                .remainingLeaveDays(employee.getAnnualLeaveDays() - usedDays)
                .status(employee.getStatus())
                .createdAt(employee.getCreatedAt())
                .build();

        return ResponseEntity.ok(EmployeeResponse.builder()
                .message("Profile retrieved successfully")
                .data(profile)
                .timestamp(Instant.now())
                .build());
    }
}
