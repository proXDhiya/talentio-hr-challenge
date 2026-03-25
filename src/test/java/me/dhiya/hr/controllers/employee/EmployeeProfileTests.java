package me.dhiya.hr.controllers.employee;

import me.dhiya.hr.controllers.BaseControllerTest;
import me.dhiya.hr.domain.EmployeeEntity;
import me.dhiya.hr.domain.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class EmployeeProfileTests extends BaseControllerTest {

    private static final String URL = "/apis/v1/employees/me";

    private String validToken;
    private EmployeeEntity savedEmployee;

    @BeforeEach
    void createUser() {
        savedEmployee = saveEmployee("John", "Doe", "john@company.com", Role.EMPLOYEE);
        validToken = token(savedEmployee);
    }

    @Test
    void profileReturns200WithEmployeeDataOnValidToken() throws Exception {
        mockMvc.perform(get(URL).header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Profile retrieved successfully"))
                .andExpect(jsonPath("$.data.id").value(savedEmployee.getId()))
                .andExpect(jsonPath("$.data.email").value(savedEmployee.getEmail()))
                .andExpect(jsonPath("$.data.firstName").value(savedEmployee.getFirstName()))
                .andExpect(jsonPath("$.data.lastName").value(savedEmployee.getLastName()))
                .andExpect(jsonPath("$.data.annualLeaveDays").value(30))
                .andExpect(jsonPath("$.data.usedLeaveDays").value(0))
                .andExpect(jsonPath("$.data.remainingLeaveDays").value(30))
                .andExpect(jsonPath("$.data.salary").value(0))
                .andExpect(jsonPath("$.data.manager").doesNotExist())
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void profileReturns401WhenNoTokenProvided() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void profileReturns401WhenTokenIsInvalid() throws Exception {
        mockMvc.perform(get(URL).header("Authorization", "Bearer invalid.token.here"))
                .andExpect(status().isUnauthorized());
    }
}
