package me.dhiya.hr.controllers.auth;

import me.dhiya.hr.controllers.BaseControllerTest;
import org.springframework.http.MediaType;
import org.junit.jupiter.api.BeforeEach;
import me.dhiya.hr.domain.enums.Role;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class AuthLoginTests extends BaseControllerTest {

    private static final String URL = "/apis/v1/auth/login";
    private static final String VALID_EMAIL = "user@company.com";

    @BeforeEach
    void createUser() {
        saveEmployee("Test", "User", VALID_EMAIL, Role.EMPLOYEE);
    }

    @Test
    void loginReturns200WithTokenOnValidCredentials() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "%s"}
                                """.formatted(VALID_EMAIL, "Pass123!")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresIn").value(900000))
                .andExpect(jsonPath("$.data.employee.email").value(VALID_EMAIL))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void loginReturns400WhenEmailIsInvalidFormat() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "not-an-email", "password": "Pass123!"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors[0].field").value("email"));
    }

    @Test
    void loginReturns400WhenEmailHasNoTld() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "user@nodomain", "password": "Pass123!"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("email"));
    }

    @Test
    void loginReturns400WhenPasswordIsTooShort() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "short"}
                                """.formatted(VALID_EMAIL)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("password"));
    }

    @Test
    void loginReturns400WhenPasswordIsTooLong() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "%s"}
                                """.formatted(VALID_EMAIL, "Aa1!".repeat(17))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("password"));
    }

    @Test
    void loginReturns400WhenEmailIsMissing() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"password": "Pass123!"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("email"));
    }

    @Test
    void loginReturns400WhenPasswordIsMissing() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s"}
                                """.formatted(VALID_EMAIL)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("password"));
    }

    @Test
    void loginReturns401WhenPasswordIsWrong() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "WrongPassword1!"}
                                """.formatted(VALID_EMAIL)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"))
                .andExpect(jsonPath("$.errors").isEmpty());
    }

    @Test
    void loginReturns401WhenEmailDoesNotExist() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "nobody@company.com", "password": "Pass123!"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }
}
