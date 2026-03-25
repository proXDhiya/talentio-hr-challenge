package me.dhiya.hr.dto.auth.response;

import me.dhiya.hr.dto.employee.response.EmployeeDto;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {
    private String accessToken;
    private String tokenType;
    private long expiresIn;
    private EmployeeDto employee;
}
