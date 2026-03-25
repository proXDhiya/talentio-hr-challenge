package me.dhiya.hr.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import me.dhiya.hr.domain.CurrencyEntity;
import me.dhiya.hr.domain.EmployeeEntity;
import me.dhiya.hr.domain.enums.EmployeeStatus;
import me.dhiya.hr.domain.enums.Role;
import me.dhiya.hr.dto.employee.request.UpdateEmployeeRequest;
import me.dhiya.hr.repositories.CurrencyRepository;
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

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class EmployeeUpdateIntegrationTests {

    private static final String URL = "/apis/v1/employees/{id}";

    @Autowired private WebApplicationContext wac;
    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private CurrencyRepository currencyRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private MockMvc mockMvc;
    private String managerToken;
    private String hrToken;
    private String employeeToken;
    private EmployeeEntity savedManager;
    private EmployeeEntity savedHr;
    private EmployeeEntity savedEmployee;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        savedManager = save("John", "Doe", "manager@company.com", Role.MANAGER);
        managerToken = jwtService.generateToken(savedManager);

        savedHr = save("Jane", "Smith", "hr@company.com", Role.HR);
        hrToken = jwtService.generateToken(savedHr);

        savedEmployee = save("Bob", "Jones", "bob@company.com", Role.EMPLOYEE);
        employeeToken = jwtService.generateToken(savedEmployee);
    }


    @Test
    void updateEmployeeReturns401WhenNoTokenProvided() throws Exception {
        mockMvc.perform(put(URL, savedEmployee.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updateEmployeeReturns403WhenCalledByEmployee() throws Exception {
        mockMvc.perform(put(URL, savedEmployee.getId())
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied."));
    }

    @Test
    void updateEmployeeReturns200WhenManagerUpdatesEmployee() throws Exception {
        UpdateEmployeeRequest request = UpdateEmployeeRequest.builder()
                .firstName("Updated")
                .build();

        mockMvc.perform(put(URL, savedEmployee.getId())
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Employee updated successfully"));
    }

    @Test
    void updateEmployeeReturns200WhenManagerUpdatesHr() throws Exception {
        UpdateEmployeeRequest request = UpdateEmployeeRequest.builder()
                .firstName("Updated")
                .build();

        mockMvc.perform(put(URL, savedHr.getId())
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void updateEmployeeReturns200WhenManagerUpdatesAnotherManager() throws Exception {
        EmployeeEntity anotherManager = save("Alice", "Lee", "alice@company.com", Role.MANAGER);

        UpdateEmployeeRequest request = UpdateEmployeeRequest.builder()
                .firstName("Updated")
                .build();

        mockMvc.perform(put(URL, anotherManager.getId())
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    void updateEmployeeReturns200WhenHrUpdatesEmployee() throws Exception {
        UpdateEmployeeRequest request = UpdateEmployeeRequest.builder()
                .firstName("Updated")
                .build();

        mockMvc.perform(put(URL, savedEmployee.getId())
                        .header("Authorization", "Bearer " + hrToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }


    @Test
    void updateEmployeeReturns403WhenHrTriesToUpdateManager() throws Exception {
        UpdateEmployeeRequest request = UpdateEmployeeRequest.builder()
                .firstName("Updated")
                .build();

        mockMvc.perform(put(URL, savedManager.getId())
                        .header("Authorization", "Bearer " + hrToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied."));
    }

    @Test
    void updateEmployeeReturns403WhenHrTriesToUpdateAnotherHr() throws Exception {
        EmployeeEntity anotherHr = save("Tom", "Green", "tom@company.com", Role.HR);

        UpdateEmployeeRequest request = UpdateEmployeeRequest.builder()
                .firstName("Updated")
                .build();

        mockMvc.perform(put(URL, anotherHr.getId())
                        .header("Authorization", "Bearer " + hrToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied."));
    }


    @Test
    void updateEmployeeReturns404WhenEmployeeDoesNotExist() throws Exception {
        mockMvc.perform(put(URL, "00000000-0000-0000-0000-000000000000")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Employee not found"));
    }

    @Test
    void updateEmployeeReturns404WhenManagerIdDoesNotExist() throws Exception {
        UpdateEmployeeRequest request = UpdateEmployeeRequest.builder()
                .managerId("00000000-0000-0000-0000-000000000000")
                .build();

        mockMvc.perform(put(URL, savedEmployee.getId())
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Manager not found"));
    }


    @Test
    void updateEmployeeReturns400WhenSalaryIsNegative() throws Exception {
        UpdateEmployeeRequest request = UpdateEmployeeRequest.builder()
                .salary(new BigDecimal("-100"))
                .build();

        mockMvc.perform(put(URL, savedEmployee.getId())
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors[0].field").value("salary"));
    }

    @Test
    void updateEmployeeReturns400WhenAnnualLeaveDaysIsZero() throws Exception {
        UpdateEmployeeRequest request = UpdateEmployeeRequest.builder()
                .annualLeaveDays(0)
                .build();

        mockMvc.perform(put(URL, savedEmployee.getId())
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("annualLeaveDays"));
    }

    @Test
    void updateEmployeeReturns400WhenFirstNameIsBlank() throws Exception {
        UpdateEmployeeRequest request = UpdateEmployeeRequest.builder()
                .firstName("")
                .build();

        mockMvc.perform(put(URL, savedEmployee.getId())
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("firstName"));
    }

    @Test
    void updateEmployeeReturns400WhenManagerIdIsNotUUID() throws Exception {
        UpdateEmployeeRequest request = UpdateEmployeeRequest.builder()
                .managerId("not-a-uuid")
                .build();

        mockMvc.perform(put(URL, savedEmployee.getId())
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("managerId"));
    }

    @Test
    void updateEmployeeReturns400WhenCurrencyCodeIsUnknown() throws Exception {
        UpdateEmployeeRequest request = UpdateEmployeeRequest.builder()
                .currencyCode("XYZ")
                .build();

        mockMvc.perform(put(URL, savedEmployee.getId())
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Currency not found: XYZ"));
    }


    @Test
    void updateEmployeeAppliesOnlyProvidedFields() throws Exception {
        UpdateEmployeeRequest request = UpdateEmployeeRequest.builder()
                .firstName("NewName")
                .build();

        mockMvc.perform(put(URL, savedEmployee.getId())
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.firstName").value("NewName"))
                .andExpect(jsonPath("$.data.lastName").value(savedEmployee.getLastName()));
    }


    @Test
    void updateEmployeeReturnsCorrectFields() throws Exception {
        CurrencyEntity eur = currencyRepository.findByCode("EUR")
                .orElseGet(() -> currencyRepository.save(CurrencyEntity.builder()
                        .code("EUR").name("Euro").symbol("€").build()));

        UpdateEmployeeRequest request = UpdateEmployeeRequest.builder()
                .firstName("Sara")
                .lastName("Ahmed")
                .department("Product")
                .position("Senior Frontend Developer")
                .salary(new BigDecimal("3500.00"))
                .currencyCode("EUR")
                .annualLeaveDays(25)
                .role(Role.EMPLOYEE)
                .build();

        mockMvc.perform(put(URL, savedEmployee.getId())
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(savedEmployee.getId()))
                .andExpect(jsonPath("$.data.firstName").value("Sara"))
                .andExpect(jsonPath("$.data.lastName").value("Ahmed"))
                .andExpect(jsonPath("$.data.department").value("Product"))
                .andExpect(jsonPath("$.data.position").value("Senior Frontend Developer"))
                .andExpect(jsonPath("$.data.salary").value(3500.00))
                .andExpect(jsonPath("$.data.currency.code").value("EUR"))
                .andExpect(jsonPath("$.data.currency.symbol").value("€"))
                .andExpect(jsonPath("$.data.role").value("EMPLOYEE"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));
    }


    private EmployeeEntity save(String firstName, String lastName, String email, Role role) {
        return employeeRepository.save(EmployeeEntity.builder()
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .password(passwordEncoder.encode("Pass123!"))
                .role(role)
                .department("Engineering")
                .hireDate(LocalDate.now())
                .status(EmployeeStatus.ACTIVE)
                .build());
    }
}
