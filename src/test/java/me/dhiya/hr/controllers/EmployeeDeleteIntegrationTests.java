package me.dhiya.hr.controllers;

import me.dhiya.hr.domain.EmployeeEntity;
import me.dhiya.hr.domain.enums.EmployeeStatus;
import me.dhiya.hr.domain.enums.Role;
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

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class EmployeeDeleteIntegrationTests {

    private static final String URL = "/apis/v1/employees/{id}";

    @Autowired private WebApplicationContext wac;
    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;

    private MockMvc mockMvc;
    private String managerToken;
    private String hrToken;
    private String employeeToken;
    private EmployeeEntity savedEmployee;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        EmployeeEntity manager = save("John", "Doe", "manager@company.com", Role.MANAGER, EmployeeStatus.ACTIVE);
        managerToken = jwtService.generateToken(manager);

        EmployeeEntity hr = save("Jane", "Smith", "hr@company.com", Role.HR, EmployeeStatus.ACTIVE);
        hrToken = jwtService.generateToken(hr);

        savedEmployee = save("Bob", "Jones", "bob@company.com", Role.EMPLOYEE, EmployeeStatus.ACTIVE);
        employeeToken = jwtService.generateToken(savedEmployee);
    }

    @Test
    void deleteEmployeeReturns401WhenNoTokenProvided() throws Exception {
        mockMvc.perform(delete(URL, savedEmployee.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteEmployeeReturns403WhenCalledByEmployee() throws Exception {
        mockMvc.perform(delete(URL, savedEmployee.getId())
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied."));
    }

    @Test
    void deleteEmployeeReturns200WhenCalledByManager() throws Exception {
        mockMvc.perform(delete(URL, savedEmployee.getId())
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Employee deactivated successfully"))
                .andExpect(jsonPath("$.data.id").value(savedEmployee.getId()))
                .andExpect(jsonPath("$.data.status").value("INACTIVE"))
                .andExpect(jsonPath("$.data.deletedAt").isNotEmpty())
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void deleteEmployeeReturns200WhenCalledByHr() throws Exception {
        mockMvc.perform(delete(URL, savedEmployee.getId())
                        .header("Authorization", "Bearer " + hrToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Employee deactivated successfully"));
    }

    @Test
    void deleteEmployeeReturns403WhenHrTriesToDeactivateManager() throws Exception {
        EmployeeEntity manager = save("Alice", "Lee", "alice@company.com", Role.MANAGER, EmployeeStatus.ACTIVE);

        mockMvc.perform(delete(URL, manager.getId())
                        .header("Authorization", "Bearer " + hrToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied."));
    }

    @Test
    void deleteEmployeeReturns403WhenHrTriesToDeactivateAnotherHr() throws Exception {
        EmployeeEntity anotherHr = save("Tom", "Green", "tom@company.com", Role.HR, EmployeeStatus.ACTIVE);

        mockMvc.perform(delete(URL, anotherHr.getId())
                        .header("Authorization", "Bearer " + hrToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied."));
    }

    @Test
    void deleteEmployeeReturns404WhenEmployeeDoesNotExist() throws Exception {
        mockMvc.perform(delete(URL, "00000000-0000-0000-0000-000000000000")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Employee not found"));
    }

    @Test
    void deleteEmployeeReturns409WhenEmployeeIsAlreadyInactive() throws Exception {
        EmployeeEntity inactive = save("Ghost", "User", "ghost@company.com", Role.EMPLOYEE, EmployeeStatus.INACTIVE);

        mockMvc.perform(delete(URL, inactive.getId())
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Employee is already deactivated"));
    }

    @Test
    void deleteEmployeeActuallySetsStatusToInactive() throws Exception {
        mockMvc.perform(delete(URL, savedEmployee.getId())
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk());

        EmployeeEntity updated = employeeRepository.findByIdNative(savedEmployee.getId()).orElseThrow();
        assert updated.getStatus() == EmployeeStatus.INACTIVE;
        assert updated.getDeletedAt() != null;
    }

    private EmployeeEntity save(String firstName, String lastName, String email, Role role, EmployeeStatus status) {
        return employeeRepository.save(EmployeeEntity.builder()
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .password(passwordEncoder.encode("Pass123!"))
                .role(role)
                .department("Engineering")
                .hireDate(LocalDate.now())
                .status(status)
                .build());
    }
}
