package me.dhiya.hr.controllers.employee;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import me.dhiya.hr.controllers.BaseControllerTest;
import me.dhiya.hr.domain.CurrencyEntity;
import me.dhiya.hr.domain.EmployeeEntity;
import me.dhiya.hr.domain.enums.Role;
import me.dhiya.hr.dto.employee.request.UpdateEmployeeRequest;
import me.dhiya.hr.repositories.CurrencyRepository;
import java.math.BigDecimal;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class EmployeeUpdateTests extends BaseControllerTest {

    private static final String URL = "/apis/v1/employees/{id}";

    @Autowired private CurrencyRepository currencyRepository;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private String managerToken;
    private String hrToken;
    private String employeeToken;
    private EmployeeEntity savedManager;
    private EmployeeEntity savedHr;
    private EmployeeEntity savedEmployee;

    @BeforeEach
    void createUsers() {
        savedManager = saveEmployee("John", "Doe", "manager@company.com", Role.MANAGER);
        managerToken = token(savedManager);
        savedHr = saveEmployee("Jane", "Smith", "hr@company.com", Role.HR);
        hrToken = token(savedHr);
        savedEmployee = saveEmployee("Bob", "Jones", "bob@company.com", Role.EMPLOYEE);
        employeeToken = token(savedEmployee);
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
        String body = objectMapper.writeValueAsString(
                UpdateEmployeeRequest.builder().firstName("Updated").build());

        mockMvc.perform(put(URL, savedEmployee.getId())
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Employee updated successfully"));
    }

    @Test
    void updateEmployeeReturns200WhenManagerUpdatesHr() throws Exception {
        String body = objectMapper.writeValueAsString(
                UpdateEmployeeRequest.builder().firstName("Updated").build());

        mockMvc.perform(put(URL, savedHr.getId())
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    @Test
    void updateEmployeeReturns200WhenManagerUpdatesAnotherManager() throws Exception {
        EmployeeEntity anotherManager = saveEmployee("Alice", "Lee", "alice@company.com", Role.MANAGER);
        String body = objectMapper.writeValueAsString(
                UpdateEmployeeRequest.builder().firstName("Updated").build());

        mockMvc.perform(put(URL, anotherManager.getId())
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    @Test
    void updateEmployeeReturns200WhenHrUpdatesEmployee() throws Exception {
        String body = objectMapper.writeValueAsString(
                UpdateEmployeeRequest.builder().firstName("Updated").build());

        mockMvc.perform(put(URL, savedEmployee.getId())
                        .header("Authorization", "Bearer " + hrToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    @Test
    void updateEmployeeReturns403WhenHrTriesToUpdateManager() throws Exception {
        String body = objectMapper.writeValueAsString(
                UpdateEmployeeRequest.builder().firstName("Updated").build());

        mockMvc.perform(put(URL, savedManager.getId())
                        .header("Authorization", "Bearer " + hrToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied."));
    }

    @Test
    void updateEmployeeReturns403WhenHrTriesToUpdateAnotherHr() throws Exception {
        EmployeeEntity anotherHr = saveEmployee("Tom", "Green", "tom@company.com", Role.HR);
        String body = objectMapper.writeValueAsString(
                UpdateEmployeeRequest.builder().firstName("Updated").build());

        mockMvc.perform(put(URL, anotherHr.getId())
                        .header("Authorization", "Bearer " + hrToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
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
        String body = objectMapper.writeValueAsString(
                UpdateEmployeeRequest.builder().managerId("00000000-0000-0000-0000-000000000000").build());

        mockMvc.perform(put(URL, savedEmployee.getId())
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Manager not found"));
    }

    @Test
    void updateEmployeeReturns400WhenSalaryIsNegative() throws Exception {
        String body = objectMapper.writeValueAsString(
                UpdateEmployeeRequest.builder().salary(new BigDecimal("-100")).build());

        mockMvc.perform(put(URL, savedEmployee.getId())
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors[0].field").value("salary"));
    }

    @Test
    void updateEmployeeReturns400WhenAnnualLeaveDaysIsZero() throws Exception {
        String body = objectMapper.writeValueAsString(
                UpdateEmployeeRequest.builder().annualLeaveDays(0).build());

        mockMvc.perform(put(URL, savedEmployee.getId())
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("annualLeaveDays"));
    }

    @Test
    void updateEmployeeReturns400WhenFirstNameIsBlank() throws Exception {
        String body = objectMapper.writeValueAsString(
                UpdateEmployeeRequest.builder().firstName("").build());

        mockMvc.perform(put(URL, savedEmployee.getId())
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("firstName"));
    }

    @Test
    void updateEmployeeReturns400WhenManagerIdIsNotUUID() throws Exception {
        String body = objectMapper.writeValueAsString(
                UpdateEmployeeRequest.builder().managerId("not-a-uuid").build());

        mockMvc.perform(put(URL, savedEmployee.getId())
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("managerId"));
    }

    @Test
    void updateEmployeeReturns400WhenCurrencyCodeIsUnknown() throws Exception {
        String body = objectMapper.writeValueAsString(
                UpdateEmployeeRequest.builder().currencyCode("XYZ").build());

        mockMvc.perform(put(URL, savedEmployee.getId())
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Currency not found: XYZ"));
    }

    @Test
    void updateEmployeeAppliesOnlyProvidedFields() throws Exception {
        String body = objectMapper.writeValueAsString(
                UpdateEmployeeRequest.builder().firstName("NewName").build());

        mockMvc.perform(put(URL, savedEmployee.getId())
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.firstName").value("NewName"))
                .andExpect(jsonPath("$.data.lastName").value(savedEmployee.getLastName()));
    }

    @Test
    void updateEmployeeReturnsCorrectFields() throws Exception {
        currencyRepository.findByCode("EUR")
                .orElseGet(() -> currencyRepository.save(CurrencyEntity.builder()
                        .code("EUR").name("Euro").symbol("€").build()));

        String body = objectMapper.writeValueAsString(UpdateEmployeeRequest.builder()
                .firstName("Sara").lastName("Ahmed").department("Product")
                .position("Senior Frontend Developer").salary(new BigDecimal("3500.00"))
                .currencyCode("EUR").annualLeaveDays(25).role(Role.EMPLOYEE).build());

        mockMvc.perform(put(URL, savedEmployee.getId())
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
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
}
