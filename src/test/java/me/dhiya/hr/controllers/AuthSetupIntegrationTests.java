package me.dhiya.hr.controllers;

import me.dhiya.hr.TestDataUtil;
import me.dhiya.hr.domain.EmployeeEntity;
import me.dhiya.hr.dto.request.SetupRequest;
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
public class AuthSetupIntegrationTests {

    private static final String SETUP_URL = "/apis/v1/auth/setup";

    @Autowired private WebApplicationContext wac;
    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @Test
    void setupReturns201WithTokenOnFirstEmployee() throws Exception {
        SetupRequest request = TestDataUtil.createSetupRequest();

        mockMvc.perform(post(SETUP_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "%s",
                                  "lastName": "%s",
                                  "email": "%s",
                                  "password": "%s",
                                  "department": "%s"
                                }
                                """.formatted(request.getFirstName(), request.getLastName(),
                                request.getEmail(), request.getPassword(), request.getDepartment())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Setup successful"))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.employee.email").value(request.getEmail()))
                .andExpect(jsonPath("$.data.employee.role").value("MANAGER"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void setupReturns409WhenEmployeeAlreadyExists() throws Exception {
        EmployeeEntity existing = TestDataUtil.createEmployee();
        existing.setPassword(passwordEncoder.encode(existing.getPassword()));
        employeeRepository.save(existing);

        SetupRequest request = TestDataUtil.createSetupRequest();

        mockMvc.perform(post(SETUP_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "%s",
                                  "lastName": "%s",
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(request.getFirstName(), request.getLastName(),
                                request.getEmail(), request.getPassword())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Setup has already been completed"));
    }

    @Test
    void setupReturns400WhenEmailIsInvalidFormat() throws Exception {
        SetupRequest request = TestDataUtil.createSetupRequest();

        mockMvc.perform(post(SETUP_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "%s",
                                  "lastName": "%s",
                                  "email": "not-an-email",
                                  "password": "%s"
                                }
                                """.formatted(request.getFirstName(), request.getLastName(), request.getPassword())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors[0].field").value("email"));
    }

    @Test
    void setupReturns400WhenEmailHasNoTld() throws Exception {
        SetupRequest request = TestDataUtil.createSetupRequest();

        mockMvc.perform(post(SETUP_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "%s",
                                  "lastName": "%s",
                                  "email": "user@nodomain",
                                  "password": "%s"
                                }
                                """.formatted(request.getFirstName(), request.getLastName(), request.getPassword())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors[0].field").value("email"));
    }

    @Test
    void setupReturns400WhenPasswordIsTooShort() throws Exception {
        SetupRequest request = TestDataUtil.createSetupRequest();

        mockMvc.perform(post(SETUP_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "%s",
                                  "lastName": "%s",
                                  "email": "%s",
                                  "password": "short"
                                }
                                """.formatted(request.getFirstName(), request.getLastName(), request.getEmail())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors[0].field").value("password"));
    }

    @Test
    void setupReturns400WhenPasswordIsTooLong() throws Exception {
        SetupRequest request = TestDataUtil.createSetupRequest();
        String tooLong = "Aa1!".repeat(17);

        mockMvc.perform(post(SETUP_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "%s",
                                  "lastName": "%s",
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(request.getFirstName(), request.getLastName(), request.getEmail(), tooLong)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors[0].field").value("password"));
    }

    @Test
    void setupReturns400WhenFirstNameIsMissing() throws Exception {
        SetupRequest request = TestDataUtil.createSetupRequest();

        mockMvc.perform(post(SETUP_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "lastName": "%s",
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(request.getLastName(), request.getEmail(), request.getPassword())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors[0].field").value("firstName"));
    }

    @Test
    void setupReturns400WhenLastNameIsMissing() throws Exception {
        SetupRequest request = TestDataUtil.createSetupRequest();

        mockMvc.perform(post(SETUP_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "%s",
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(request.getFirstName(), request.getEmail(), request.getPassword())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors[0].field").value("lastName"));
    }

    @Test
    void setupReturns400WhenEmailIsMissing() throws Exception {
        SetupRequest request = TestDataUtil.createSetupRequest();

        mockMvc.perform(post(SETUP_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "%s",
                                  "lastName": "%s",
                                  "password": "%s"
                                }
                                """.formatted(request.getFirstName(), request.getLastName(), request.getPassword())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors[0].field").value("email"));
    }

    @Test
    void setupReturns400WhenPasswordIsMissing() throws Exception {
        SetupRequest request = TestDataUtil.createSetupRequest();

        mockMvc.perform(post(SETUP_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "%s",
                                  "lastName": "%s",
                                  "email": "%s"
                                }
                                """.formatted(request.getFirstName(), request.getLastName(), request.getEmail())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors[0].field").value("password"));
    }
}
