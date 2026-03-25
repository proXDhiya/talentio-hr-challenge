package me.dhiya.hr.controllers;

import me.dhiya.hr.TestDataUtil;
import me.dhiya.hr.domain.EmployeeEntity;
import me.dhiya.hr.domain.enums.Role;
import me.dhiya.hr.repositories.EmployeeRepository;
import me.dhiya.hr.services.JwtService;
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

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class EmployeeCreateIntegrationTests {

    private static final String URL = "/apis/v1/employees";

    @Autowired private WebApplicationContext wac;
    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;

    private MockMvc mockMvc;
    private String managerToken;
    private String hrToken;
    private String employeeToken;
    private EmployeeEntity savedManager;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        EmployeeEntity manager = TestDataUtil.createEmployee();
        manager.setRole(Role.MANAGER);
        manager.setPassword(passwordEncoder.encode(manager.getPassword()));
        savedManager = employeeRepository.save(manager);
        managerToken = jwtService.generateToken(savedManager);

        EmployeeEntity hr = TestDataUtil.createEmployee();
        hr.setRole(Role.HR);
        hr.setPassword(passwordEncoder.encode(hr.getPassword()));
        hrToken = jwtService.generateToken(employeeRepository.save(hr));

        EmployeeEntity emp = TestDataUtil.createEmployee();
        emp.setPassword(passwordEncoder.encode(emp.getPassword()));
        employeeToken = jwtService.generateToken(employeeRepository.save(emp));
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
                                {
                                  "firstName": "Sara",
                                  "lastName": "Ahmed",
                                  "email": "not-an-email",
                                  "password": "TempPass123!",
                                  "role": "EMPLOYEE",
                                  "department": "Engineering",
                                  "position": "Software Engineer",
                                  "salary": 3000.00,
                                  "currencyCode": "USD",
                                  "hireDate": "%s"
                                }
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
                                {
                                  "firstName": "Sara",
                                  "lastName": "Ahmed",
                                  "email": "sara@company.com",
                                  "password": "nouppercase1",
                                  "role": "EMPLOYEE",
                                  "department": "Engineering",
                                  "position": "Software Engineer",
                                  "salary": 3000.00,
                                  "currencyCode": "USD",
                                  "hireDate": "%s"
                                }
                                """.formatted(LocalDate.now())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors[0].field").value("password"));
    }

    @Test
    void createEmployeeReturns400WhenPasswordHasNoNumber() throws Exception {
        mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Sara",
                                  "lastName": "Ahmed",
                                  "email": "sara@company.com",
                                  "password": "NoNumberHere!",
                                  "role": "EMPLOYEE",
                                  "department": "Engineering",
                                  "position": "Software Engineer",
                                  "salary": 3000.00,
                                  "currencyCode": "USD",
                                  "hireDate": "%s"
                                }
                                """.formatted(LocalDate.now())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors[0].field").value("password"));
    }

    @Test
    void createEmployeeReturns400WhenFirstNameIsTooShort() throws Exception {
        mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "S",
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
                                """.formatted(LocalDate.now())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors[0].field").value("firstName"));
    }

    @Test
    void createEmployeeReturns400WhenSalaryIsNegative() throws Exception {
        mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Sara",
                                  "lastName": "Ahmed",
                                  "email": "sara@company.com",
                                  "password": "TempPass123!",
                                  "role": "EMPLOYEE",
                                  "department": "Engineering",
                                  "position": "Software Engineer",
                                  "salary": -100,
                                  "currencyCode": "USD",
                                  "hireDate": "%s"
                                }
                                """.formatted(LocalDate.now())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors[0].field").value("salary"));
    }

    @Test
    void createEmployeeReturns400WhenHireDateIsInFuture() throws Exception {
        mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
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
                                """.formatted(LocalDate.now().plusDays(1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors[0].field").value("hireDate"));
    }

    @Test
    void createEmployeeReturns400WhenCurrencyCodeDoesNotExist() throws Exception {
        mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "firstName": "Sara",
                                  "lastName": "Ahmed",
                                  "email": "sara@company.com",
                                  "password": "TempPass123!",
                                  "role": "EMPLOYEE",
                                  "department": "Engineering",
                                  "position": "Software Engineer",
                                  "salary": 3000.00,
                                  "currencyCode": "XYZ",
                                  "hireDate": "%s"
                                }
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
