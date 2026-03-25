package me.dhiya.hr.controllers;

import me.dhiya.hr.TestDataUtil;
import me.dhiya.hr.domain.EmployeeEntity;
import me.dhiya.hr.repositories.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class AuthLoginIntegrationTests {

    private static final String LOGIN_URL = "/apis/v1/auth/login";

    @Autowired private WebApplicationContext wac;
    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private MockMvc mockMvc;
    private String validEmail;
    private String validPassword;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        EmployeeEntity employee = TestDataUtil.createEmployee();
        validEmail = employee.getEmail();
        validPassword = employee.getPassword();
        employee.setPassword(passwordEncoder.encode(validPassword));
        employeeRepository.save(employee);
    }

    @Test
    void loginReturns200WithTokenOnValidCredentials() throws Exception {
        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "%s"}
                                """.formatted(validEmail, validPassword)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresIn").value(900000))
                .andExpect(jsonPath("$.data.employee.email").value(validEmail))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void loginReturns400WhenEmailIsInvalidFormat() throws Exception {
        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "not-an-email", "password": "%s"}
                                """.formatted(validPassword)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors[0].field").value("email"));
    }

    @Test
    void loginReturns400WhenEmailHasNoTld() throws Exception {
        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "user@nodomain", "password": "%s"}
                                """.formatted(validPassword)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors[0].field").value("email"));
    }

    @Test
    void loginReturns400WhenPasswordIsTooShort() throws Exception {
        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "short"}
                                """.formatted(validEmail)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors[0].field").value("password"));
    }

    @Test
    void loginReturns400WhenPasswordIsTooLong() throws Exception {
        String tooLong = "Aa1!".repeat(17);
        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "%s"}
                                """.formatted(validEmail, tooLong)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors[0].field").value("password"));
    }

    @Test
    void loginReturns400WhenEmailIsMissing() throws Exception {
        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"password": "%s"}
                                """.formatted(validPassword)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors[0].field").value("email"));
    }

    @Test
    void loginReturns400WhenPasswordIsMissing() throws Exception {
        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s"}
                                """.formatted(validEmail)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors[0].field").value("password"));
    }

    @Test
    void loginReturns401WhenPasswordIsWrong() throws Exception {
        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "WrongPassword1!"}
                                """.formatted(validEmail)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"))
                .andExpect(jsonPath("$.errors").isEmpty());
    }

    @Test
    void loginReturns401WhenEmailDoesNotExist() throws Exception {
        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "nobody@company.com", "password": "%s"}
                                """.formatted(validPassword)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"))
                .andExpect(jsonPath("$.errors").isEmpty());
    }
}
