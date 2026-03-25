package me.dhiya.hr.controllers.auth;

import me.dhiya.hr.TestDataUtil;
import me.dhiya.hr.controllers.BaseControllerTest;
import me.dhiya.hr.dto.auth.request.SetupRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class AuthSetupTests extends BaseControllerTest {

    private static final String URL = "/apis/v1/auth/setup";

    @Test
    void setupReturns201WithTokenOnFirstEmployee() throws Exception {
        SetupRequest request = TestDataUtil.createSetupRequest();

        mockMvc.perform(post(URL)
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
        saveEmployee("Existing", "User", "existing@company.com", me.dhiya.hr.domain.enums.Role.EMPLOYEE);
        SetupRequest request = TestDataUtil.createSetupRequest();

        mockMvc.perform(post(URL)
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
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Setup has already been completed"));
    }

    @Test
    void setupReturns400WhenEmailIsInvalidFormat() throws Exception {
        SetupRequest request = TestDataUtil.createSetupRequest();

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "%s",
                                  "lastName": "%s",
                                  "email": "not-an-email",
                                  "password": "%s",
                                  "department": "%s"
                                }
                                """.formatted(request.getFirstName(), request.getLastName(),
                                request.getPassword(), request.getDepartment())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("email"));
    }

    @Test
    void setupReturns400WhenEmailHasNoTld() throws Exception {
        SetupRequest request = TestDataUtil.createSetupRequest();

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "%s",
                                  "lastName": "%s",
                                  "email": "user@nodomain",
                                  "password": "%s",
                                  "department": "%s"
                                }
                                """.formatted(request.getFirstName(), request.getLastName(),
                                request.getPassword(), request.getDepartment())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("email"));
    }

    @Test
    void setupReturns400WhenPasswordIsTooShort() throws Exception {
        SetupRequest request = TestDataUtil.createSetupRequest();

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "%s",
                                  "lastName": "%s",
                                  "email": "%s",
                                  "password": "short",
                                  "department": "%s"
                                }
                                """.formatted(request.getFirstName(), request.getLastName(),
                                request.getEmail(), request.getDepartment())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("password"));
    }

    @Test
    void setupReturns400WhenPasswordIsTooLong() throws Exception {
        SetupRequest request = TestDataUtil.createSetupRequest();

        mockMvc.perform(post(URL)
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
                                request.getEmail(), "Aa1!".repeat(17), request.getDepartment())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("password"));
    }

    @Test
    void setupReturns400WhenPasswordHasNoUppercase() throws Exception {
        SetupRequest request = TestDataUtil.createSetupRequest();

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "%s",
                                  "lastName": "%s",
                                  "email": "%s",
                                  "password": "nouppercase1",
                                  "department": "%s"
                                }
                                """.formatted(request.getFirstName(), request.getLastName(),
                                request.getEmail(), request.getDepartment())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("password"));
    }

    @Test
    void setupReturns400WhenPasswordHasNoNumber() throws Exception {
        SetupRequest request = TestDataUtil.createSetupRequest();

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "%s",
                                  "lastName": "%s",
                                  "email": "%s",
                                  "password": "NoNumberHere!",
                                  "department": "%s"
                                }
                                """.formatted(request.getFirstName(), request.getLastName(),
                                request.getEmail(), request.getDepartment())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("password"));
    }

    @Test
    void setupReturns400WhenFirstNameIsMissing() throws Exception {
        SetupRequest request = TestDataUtil.createSetupRequest();

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "lastName": "%s",
                                  "email": "%s",
                                  "password": "%s",
                                  "department": "%s"
                                }
                                """.formatted(request.getLastName(), request.getEmail(),
                                request.getPassword(), request.getDepartment())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("firstName"));
    }

    @Test
    void setupReturns400WhenLastNameIsMissing() throws Exception {
        SetupRequest request = TestDataUtil.createSetupRequest();

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "%s",
                                  "email": "%s",
                                  "password": "%s",
                                  "department": "%s"
                                }
                                """.formatted(request.getFirstName(), request.getEmail(),
                                request.getPassword(), request.getDepartment())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("lastName"));
    }

    @Test
    void setupReturns400WhenEmailIsMissing() throws Exception {
        SetupRequest request = TestDataUtil.createSetupRequest();

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "%s",
                                  "lastName": "%s",
                                  "password": "%s",
                                  "department": "%s"
                                }
                                """.formatted(request.getFirstName(), request.getLastName(),
                                request.getPassword(), request.getDepartment())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("email"));
    }

    @Test
    void setupReturns400WhenPasswordIsMissing() throws Exception {
        SetupRequest request = TestDataUtil.createSetupRequest();

        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "%s",
                                  "lastName": "%s",
                                  "email": "%s",
                                  "department": "%s"
                                }
                                """.formatted(request.getFirstName(), request.getLastName(),
                                request.getEmail(), request.getDepartment())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("password"));
    }
}
