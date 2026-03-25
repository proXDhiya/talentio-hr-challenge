package me.dhiya.hr.controllers.employee;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import me.dhiya.hr.controllers.BaseControllerTest;
import me.dhiya.hr.domain.CurrencyEntity;
import me.dhiya.hr.domain.EmployeeEntity;
import me.dhiya.hr.domain.enums.EmployeeStatus;
import me.dhiya.hr.domain.enums.Role;
import me.dhiya.hr.repositories.CurrencyRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class EmployeeGetByIdTests extends BaseControllerTest {

    private static final String URL = "/apis/v1/employees/{id}";

    @Autowired private CurrencyRepository currencyRepository;

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

    @Test
    void getEmployeeByIdReturns200WhenCalledByManager() throws Exception {
        EmployeeEntity target = saveEmployee("Sara", "Ahmed", "sara@company.com", Role.EMPLOYEE);

        mockMvc.perform(get(URL, target.getId()).header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Employee retrieved successfully"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void getEmployeeByIdReturns200WhenCalledByHr() throws Exception {
        EmployeeEntity target = saveEmployee("Sara", "Ahmed", "sara@company.com", Role.EMPLOYEE);

        mockMvc.perform(get(URL, target.getId()).header("Authorization", "Bearer " + hrToken))
                .andExpect(status().isOk());
    }

    @Test
    void getEmployeeByIdReturns401WhenNoTokenProvided() throws Exception {
        mockMvc.perform(get(URL, savedManager.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getEmployeeByIdReturns403WhenCalledByEmployee() throws Exception {
        mockMvc.perform(get(URL, savedManager.getId()).header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied."));
    }

    @Test
    void getEmployeeByIdReturns404WhenEmployeeDoesNotExist() throws Exception {
        mockMvc.perform(get(URL, "00000000-0000-0000-0000-000000000000")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Employee not found"));
    }

    @Test
    void getEmployeeByIdReturns404WhenEmployeeIsInactive() throws Exception {
        EmployeeEntity inactive = saveEmployee("Ghost", "User", "ghost@company.com", Role.EMPLOYEE, EmployeeStatus.INACTIVE);

        mockMvc.perform(get(URL, inactive.getId()).header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void getEmployeeByIdReturnsAllFields() throws Exception {
        CurrencyEntity usd = currencyRepository.findByCode("USD")
                .orElseGet(() -> currencyRepository.save(CurrencyEntity.builder()
                        .code("USD").name("US Dollar").symbol("$").build()));

        EmployeeEntity emp = EmployeeEntity.builder()
                .firstName("Sara")
                .lastName("Ahmed")
                .email("sara@company.com")
                .password(passwordEncoder.encode("Pass123!"))
                .role(Role.EMPLOYEE)
                .department("Engineering")
                .position("Frontend Developer")
                .salary(new BigDecimal("3000.00"))
                .currency(usd)
                .hireDate(LocalDate.of(2026, 3, 25))
                .annualLeaveDays(30)
                .manager(savedManager)
                .build();
        EmployeeEntity saved = employeeRepository.save(emp);

        mockMvc.perform(get(URL, saved.getId()).header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(saved.getId()))
                .andExpect(jsonPath("$.data.firstName").value("Sara"))
                .andExpect(jsonPath("$.data.lastName").value("Ahmed"))
                .andExpect(jsonPath("$.data.email").value("sara@company.com"))
                .andExpect(jsonPath("$.data.role").value("EMPLOYEE"))
                .andExpect(jsonPath("$.data.department").value("Engineering"))
                .andExpect(jsonPath("$.data.position").value("Frontend Developer"))
                .andExpect(jsonPath("$.data.salary").value(3000.00))
                .andExpect(jsonPath("$.data.currency.code").value("USD"))
                .andExpect(jsonPath("$.data.currency.symbol").value("$"))
                .andExpect(jsonPath("$.data.hireDate").value("2026-03-25"))
                .andExpect(jsonPath("$.data.annualLeaveDays").value(30))
                .andExpect(jsonPath("$.data.usedLeaveDays").value(0))
                .andExpect(jsonPath("$.data.remainingLeaveDays").value(30))
                .andExpect(jsonPath("$.data.manager.id").value(savedManager.getId()))
                .andExpect(jsonPath("$.data.manager.firstName").value("John"))
                .andExpect(jsonPath("$.data.manager.lastName").value("Doe"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.createdAt").isNotEmpty());
    }

    @Test
    void getEmployeeByIdReturnsNullManagerWhenNoManagerAssigned() throws Exception {
        EmployeeEntity standalone = saveEmployee("Solo", "Dev", "solo@company.com", Role.EMPLOYEE);

        mockMvc.perform(get(URL, standalone.getId()).header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.manager").doesNotExist());
    }
}
