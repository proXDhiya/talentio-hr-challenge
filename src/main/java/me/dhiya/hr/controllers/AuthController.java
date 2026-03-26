package me.dhiya.hr.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import me.dhiya.hr.config.JwtProperties;
import me.dhiya.hr.domain.EmployeeEntity;
import me.dhiya.hr.dto.auth.request.LoginRequest;
import me.dhiya.hr.dto.auth.request.SetupRequest;
import me.dhiya.hr.dto.auth.response.AuthResponse;
import me.dhiya.hr.dto.auth.response.LoginResponse;
import me.dhiya.hr.dto.employee.response.EmployeeDto;
import me.dhiya.hr.mappers.Mapper;
import me.dhiya.hr.services.EmployeeService;
import me.dhiya.hr.services.JwtService;
import me.dhiya.hr.util.ApiExamples;
import java.time.Instant;

@RestController
@RequestMapping("/apis/v1/auth")
@Tag(name = "Authentication", description = "Public endpoints for system setup and login. No token required.")
public class AuthController {

    private final Mapper<EmployeeEntity, EmployeeDto> employeeMapper;
    private final EmployeeService employeeService;
    private final JwtProperties jwtProperties;
    private final JwtService jwtService;

    public AuthController(
            EmployeeService employeeService,
            JwtService jwtService,
            Mapper<EmployeeEntity, EmployeeDto> employeeMapper,
            JwtProperties jwtProperties
    ) {
        this.employeeService = employeeService;
        this.jwtService = jwtService;
        this.employeeMapper = employeeMapper;
        this.jwtProperties = jwtProperties;
    }

    @Operation(
            summary = "System Setup",
            description = "One-time bootstrap endpoint that creates the first MANAGER account.\n\n" +
                    "**Notes:**\n" +
                    "* Can only be called once - returns `409` if any employee already exists\n" +
                    "* The created account is assigned the **MANAGER** role automatically\n" +
                    "* Use the returned token or call `POST /auth/login` to authenticate"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Setup successful"),
            @ApiResponse(responseCode = "400", description = "Validation failed",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = ApiExamples.VALIDATION_ERROR))),
            @ApiResponse(responseCode = "409", description = "Setup already completed",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = ApiExamples.CONFLICT)))
    })
    @PostMapping(value = "/setup", produces = "application/json")
    public ResponseEntity<AuthResponse> setup(@Valid @RequestBody SetupRequest request) {
        EmployeeEntity employee = EmployeeEntity.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(request.getPassword())
                .department(request.getDepartment())
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(
                buildAuthResponse("Setup successful", employeeService.setup(employee)));
    }

    @Operation(
            summary = "Login",
            description = "Authenticate with email and password. Returns a Bearer JWT token valid for **15 minutes**.\n\n" +
                    "**Notes:**\n" +
                    "* Include the token in every request as: `Authorization: Bearer <token>`\n" +
                    "* Returns `401` if credentials are wrong or the employee is deactivated"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful"),
            @ApiResponse(responseCode = "400", description = "Validation failed",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = ApiExamples.VALIDATION_ERROR))),
            @ApiResponse(responseCode = "401", description = "Invalid credentials",
                    content = @Content(mediaType = "application/json",
                            examples = @ExampleObject(value = ApiExamples.UNAUTHORIZED)))
    })
    @PostMapping(value = "/login", produces = "application/json")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        EmployeeEntity employee = employeeService.authenticate(request.getEmail(), request.getPassword())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        return ResponseEntity.ok(buildAuthResponse("Login successful", employee));
    }

    private AuthResponse buildAuthResponse(String message, EmployeeEntity employee) {
        return AuthResponse.builder()
                .message(message)
                .data(LoginResponse.builder()
                        .accessToken(jwtService.generateToken(employee))
                        .tokenType("Bearer")
                        .expiresIn(jwtProperties.getAccessTokenExpiration())
                        .employee(employeeMapper.mapTo(employee))
                        .build())
                .timestamp(Instant.now())
                .build();
    }
}
