package me.dhiya.hr.controllers;

import me.dhiya.hr.TestDataUtil;
import me.dhiya.hr.domain.EmployeeEntity;
import me.dhiya.hr.repositories.EmployeeRepository;
import me.dhiya.hr.services.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class EmployeeProfileIntegrationTests {

    private static final String PROFILE_URL = "/apis/v1/employees/me";

    @Autowired private WebApplicationContext wac;
    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;

    private MockMvc mockMvc;
    private EmployeeEntity savedEmployee;
    private String validToken;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        EmployeeEntity employee = TestDataUtil.createEmployee();
        employee.setPassword(passwordEncoder.encode(employee.getPassword()));
        savedEmployee = employeeRepository.save(employee);
        validToken = jwtService.generateToken(savedEmployee);
    }

    @Test
    void profileReturns200WithEmployeeDataOnValidToken() throws Exception {
        mockMvc.perform(get(PROFILE_URL)
                        .header("Authorization", "Bearer " + validToken))
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
        mockMvc.perform(get(PROFILE_URL))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void profileReturns401WhenTokenIsInvalid() throws Exception {
        mockMvc.perform(get(PROFILE_URL)
                        .header("Authorization", "Bearer invalid.token.here"))
                .andExpect(status().isUnauthorized());
    }
}
