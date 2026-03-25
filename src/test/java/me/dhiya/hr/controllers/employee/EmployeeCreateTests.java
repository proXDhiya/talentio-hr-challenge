package me.dhiya.hr.controllers.employee;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import me.dhiya.hr.controllers.BaseControllerTest;
import me.dhiya.hr.domain.EmployeeEntity;
import me.dhiya.hr.domain.enums.Role;
import java.time.LocalDate;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class EmployeeCreateTests extends BaseControllerTest {

    private static final String URL = "/apis/v1/employees";

    private String managerToken;
    private String hrToken;
    private String employeeToken;
    private EmployeeEntity savedManager;

    @BeforeEach
    void createUsers() {
        savedManager = saveEmployee("John", "Doe", "manager@company.com", Role.MANAGER);
        managerToken = token(savedManager);
        hrToken = token(saveEmployee("Jane", "Smith", "hr@company.com", Role.HR));
        employeeToken = token(saveEmployee("Bob", "Jones", "bob@company.com", Role.EMPLOYEE));
    }

    private String validRequest() {
        return """
                {
                  "firstName": "Sara",
                  "lastName": "Ahmed",
                  "email": "sara@company.com",
                  "password": "TempPass123!",
                  "role": "EMPLOYEE",
                  "department": "Engineering",
                  "position": "Software Engineer",
                  "salary": 3000.00,
                  "currencyCode": "USD",
                  "hireDate": "%s"
                }
                """.formatted(LocalDate.now());
    }

    @Test
    void createEmployeeReturns201WhenCalledByManager() throws Exception {
        mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Employee created successfully"))
                .andExpect(jsonPath("$.data.email").value("sara@company.com"))
                .andExpect(jsonPath("$.data.firstName").value("Sara"))
                .andExpect(jsonPath("$.data.lastName").value("Ahmed"))
                .andExpect(jsonPath("$.data.role").value("EMPLOYEE"))
                .andExpect(jsonPath("$.data.salary").value(3000.00))
                .andExpect(jsonPath("$.data.currency.code").value("USD"))
                .andExpect(jsonPath("$.data.annualLeaveDays").value(30))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void createEmployeeReturns201WhenCalledByHr() throws Exception {
        mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer " + hrToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isCreated());
    }

    @Test
    void createEmployeeReturns201WithManagerAutoSetToCurrentUser() throws Exception {
        mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.manager.id").value(savedManager.getId()))
                .andExpect(jsonPath("$.data.manager.firstName").value(savedManager.getFirstName()))
                .andExpect(jsonPath("$.data.manager.lastName").value(savedManager.getLastName()));
    }

    @Test
    void createEmployeeReturns401WhenNoTokenProvided() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createEmployeeReturns403WhenCalledByEmployee() throws Exception {
        mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied."));
    }

    @Test
    void createEmployeeReturns400WhenEmailIsInvalid() throws Exception {
        mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Sara","lastName":"Ahmed","email":"not-an-email",
                                 "password":"TempPass123!","role":"EMPLOYEE","department":"Engineering",
                                 "position":"Dev","salary":3000,"currencyCode":"USD","hireDate":"%s"}
                                """.formatted(LocalDate.now())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors[0].field").value("email"));
    }

    @Test
    void createEmployeeReturns400WhenPasswordHasNoUppercase() throws Exception {
        mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Sara","lastName":"Ahmed","email":"sara@company.com",
                                 "password":"nouppercase1","role":"EMPLOYEE","department":"Engineering",
                                 "position":"Dev","salary":3000,"currencyCode":"USD","hireDate":"%s"}
                                """.formatted(LocalDate.now())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("password"));
    }

    @Test
    void createEmployeeReturns400WhenPasswordHasNoNumber() throws Exception {
        mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Sara","lastName":"Ahmed","email":"sara@company.com",
                                 "password":"NoNumberHere!","role":"EMPLOYEE","department":"Engineering",
                                 "position":"Dev","salary":3000,"currencyCode":"USD","hireDate":"%s"}
                                """.formatted(LocalDate.now())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("password"));
    }

    @Test
    void createEmployeeReturns400WhenFirstNameIsTooShort() throws Exception {
        mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"S","lastName":"Ahmed","email":"sara@company.com",
                                 "password":"TempPass123!","role":"EMPLOYEE","department":"Engineering",
                                 "position":"Dev","salary":3000,"currencyCode":"USD","hireDate":"%s"}
                                """.formatted(LocalDate.now())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("firstName"));
    }

    @Test
    void createEmployeeReturns400WhenSalaryIsNegative() throws Exception {
        mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Sara","lastName":"Ahmed","email":"sara@company.com",
                                 "password":"TempPass123!","role":"EMPLOYEE","department":"Engineering",
                                 "position":"Dev","salary":-100,"currencyCode":"USD","hireDate":"%s"}
                                """.formatted(LocalDate.now())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("salary"));
    }

    @Test
    void createEmployeeReturns400WhenHireDateIsInFuture() throws Exception {
        mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Sara","lastName":"Ahmed","email":"sara@company.com",
                                 "password":"TempPass123!","role":"EMPLOYEE","department":"Engineering",
                                 "position":"Dev","salary":3000,"currencyCode":"USD","hireDate":"%s"}
                                """.formatted(LocalDate.now().plusDays(1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("hireDate"));
    }

    @Test
    void createEmployeeReturns400WhenCurrencyCodeDoesNotExist() throws Exception {
        mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Sara","lastName":"Ahmed","email":"sara@company.com",
                                 "password":"TempPass123!","role":"EMPLOYEE","department":"Engineering",
                                 "position":"Dev","salary":3000,"currencyCode":"XYZ","hireDate":"%s"}
                                """.formatted(LocalDate.now())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Currency not found: XYZ"));
    }

    @Test
    void createEmployeeReturns409WhenEmailAlreadyExists() throws Exception {
        mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isCreated());

        mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("An employee with this email already exists"))
                .andExpect(jsonPath("$.errors[0].field").value("email"));
    }
}
