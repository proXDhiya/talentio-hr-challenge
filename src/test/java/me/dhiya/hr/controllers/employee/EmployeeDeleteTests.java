package me.dhiya.hr.controllers.employee;

import me.dhiya.hr.controllers.BaseControllerTest;
import me.dhiya.hr.domain.EmployeeEntity;
import me.dhiya.hr.domain.enums.EmployeeStatus;
import me.dhiya.hr.domain.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class EmployeeDeleteTests extends BaseControllerTest {

    private static final String URL = "/apis/v1/employees/{id}";

    private String managerToken;
    private String hrToken;
    private String employeeToken;
    private EmployeeEntity savedEmployee;

    @BeforeEach
    void createUsers() {
        managerToken = token(saveEmployee("John", "Doe", "manager@company.com", Role.MANAGER));
        hrToken = token(saveEmployee("Jane", "Smith", "hr@company.com", Role.HR));
        savedEmployee = saveEmployee("Bob", "Jones", "bob@company.com", Role.EMPLOYEE);
        employeeToken = token(savedEmployee);
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
        EmployeeEntity manager = saveEmployee("Alice", "Lee", "alice@company.com", Role.MANAGER);

        mockMvc.perform(delete(URL, manager.getId())
                        .header("Authorization", "Bearer " + hrToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied."));
    }

    @Test
    void deleteEmployeeReturns403WhenHrTriesToDeactivateAnotherHr() throws Exception {
        EmployeeEntity anotherHr = saveEmployee("Tom", "Green", "tom@company.com", Role.HR);

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
        EmployeeEntity inactive = saveEmployee("Ghost", "User", "ghost@company.com", Role.EMPLOYEE, EmployeeStatus.INACTIVE);

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
        assertThat(updated.getStatus()).isEqualTo(EmployeeStatus.INACTIVE);
        assertThat(updated.getDeletedAt()).isNotNull();
    }
}
