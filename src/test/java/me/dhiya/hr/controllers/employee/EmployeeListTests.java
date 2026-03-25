package me.dhiya.hr.controllers.employee;

import com.jayway.jsonpath.JsonPath;
import me.dhiya.hr.controllers.BaseControllerTest;
import me.dhiya.hr.domain.EmployeeEntity;
import me.dhiya.hr.domain.enums.EmployeeStatus;
import me.dhiya.hr.domain.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class EmployeeListTests extends BaseControllerTest {

    private static final String URL = "/apis/v1/employees";

    private String managerToken;
    private String hrToken;
    private String employeeToken;

    @BeforeEach
    void createUsers() {
        managerToken = token(saveEmployee("John", "Doe", "manager@company.com", Role.MANAGER));
        hrToken = token(saveEmployee("Jane", "Smith", "hr@company.com", Role.HR));
        employeeToken = token(saveEmployee("Bob", "Jones", "bob@company.com", Role.EMPLOYEE));
    }

    @Test
    void listEmployeesReturns200WhenCalledByManager() throws Exception {
        mockMvc.perform(get(URL).header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Employees retrieved successfully"))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.hasMore").isBoolean())
                .andExpect(jsonPath("$.data.size").isNumber())
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void listEmployeesReturns200WhenCalledByHr() throws Exception {
        mockMvc.perform(get(URL).header("Authorization", "Bearer " + hrToken))
                .andExpect(status().isOk());
    }

    @Test
    void listEmployeesReturns401WhenNoTokenProvided() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listEmployeesReturns403WhenCalledByEmployee() throws Exception {
        mockMvc.perform(get(URL).header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied."));
    }

    @Test
    void listEmployeesReturnsDefaultSizeOf20() throws Exception {
        mockMvc.perform(get(URL).header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.size").value(20));
    }

    @Test
    void listEmployeesReturnsRequestedSize() throws Exception {
        mockMvc.perform(get(URL)
                        .header("Authorization", "Bearer " + managerToken)
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.size").value(5));
    }

    @Test
    void listEmployeesReturns400WhenSizeExceedsMax() throws Exception {
        mockMvc.perform(get(URL)
                        .header("Authorization", "Bearer " + managerToken)
                        .param("size", "200"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors[0].field").value("size"));
    }

    @Test
    void listEmployeesReturns400WhenSizeIsBelowMin() throws Exception {
        mockMvc.perform(get(URL)
                        .header("Authorization", "Bearer " + managerToken)
                        .param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("size"));
    }

    @Test
    void listEmployeesReturns400WhenCursorIsNotUUID() throws Exception {
        mockMvc.perform(get(URL)
                        .header("Authorization", "Bearer " + managerToken)
                        .param("cursor", "not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("cursor"));
    }

    @Test
    void listEmployeesReturns400WhenRoleIsInvalid() throws Exception {
        mockMvc.perform(get(URL)
                        .header("Authorization", "Bearer " + managerToken)
                        .param("role", "INVALID_ROLE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("role"));
    }

    @Test
    void listEmployeesReturns400WhenSearchIsEmpty() throws Exception {
        mockMvc.perform(get(URL)
                        .header("Authorization", "Bearer " + managerToken)
                        .param("search", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("search"));
    }

    @Test
    void listEmployeesReturnsHasMoreTrueAndNextCursorWhenMorePagesExist() throws Exception {
        saveEmployee("E1", "Last", "e1@company.com", Role.EMPLOYEE);
        saveEmployee("E2", "Last", "e2@company.com", Role.EMPLOYEE);
        saveEmployee("E3", "Last", "e3@company.com", Role.EMPLOYEE);

        mockMvc.perform(get(URL)
                        .header("Authorization", "Bearer " + managerToken)
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hasMore").value(true))
                .andExpect(jsonPath("$.data.nextCursor").isNotEmpty())
                .andExpect(jsonPath("$.data.items.length()").value(2));
    }

    @Test
    void listEmployeesReturnsHasMoreFalseOnLastPage() throws Exception {
        mockMvc.perform(get(URL)
                        .header("Authorization", "Bearer " + managerToken)
                        .param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hasMore").value(false))
                .andExpect(jsonPath("$.data.nextCursor").doesNotExist());
    }

    @Test
    void listEmployeesReturnsDifferentItemsOnNextPage() throws Exception {
        saveEmployee("E1", "Last", "e1@company.com", Role.EMPLOYEE);
        saveEmployee("E2", "Last", "e2@company.com", Role.EMPLOYEE);
        saveEmployee("E3", "Last", "e3@company.com", Role.EMPLOYEE);
        saveEmployee("E4", "Last", "e4@company.com", Role.EMPLOYEE);

        MvcResult page1 = mockMvc.perform(get(URL)
                        .header("Authorization", "Bearer " + managerToken)
                        .param("size", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hasMore").value(true))
                .andReturn();

        String body1 = page1.getResponse().getContentAsString();
        String cursor = JsonPath.read(body1, "$.data.nextCursor");
        List<String> page1Ids = JsonPath.read(body1, "$.data.items[*].id");

        String body2 = mockMvc.perform(get(URL)
                        .header("Authorization", "Bearer " + managerToken)
                        .param("size", "3")
                        .param("cursor", cursor))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        List<String> page2Ids = JsonPath.read(body2, "$.data.items[*].id");
        assertThat(page2Ids).doesNotContainAnyElementsOf(page1Ids);
    }

    @Test
    void listEmployeesFiltersByDepartment() throws Exception {
        saveEmployee("Sara", "Ahmed", "sara@company.com", "Engineering", Role.EMPLOYEE, null);
        saveEmployee("Mark", "Brown", "mark@company.com", "Marketing", Role.EMPLOYEE, null);

        mockMvc.perform(get(URL)
                        .header("Authorization", "Bearer " + managerToken)
                        .param("department", "Marketing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[?(@.department == 'Engineering')]").isEmpty())
                .andExpect(jsonPath("$.data.items[?(@.department == 'Marketing')]").isNotEmpty());
    }

    @Test
    void listEmployeesFiltersByRole() throws Exception {
        mockMvc.perform(get(URL)
                        .header("Authorization", "Bearer " + managerToken)
                        .param("role", "EMPLOYEE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[?(@.role == 'MANAGER')]").isEmpty())
                .andExpect(jsonPath("$.data.items[?(@.role == 'EMPLOYEE')]").isNotEmpty());
    }

    @Test
    void listEmployeesSearchesByName() throws Exception {
        saveEmployee("Sara", "Ahmed", "sara@company.com", Role.EMPLOYEE);

        mockMvc.perform(get(URL)
                        .header("Authorization", "Bearer " + managerToken)
                        .param("search", "sara"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[?(@.firstName == 'Sara')]").isNotEmpty())
                .andExpect(jsonPath("$.data.items[?(@.firstName == 'Bob')]").isEmpty());
    }

    @Test
    void listEmployeesSearchesByEmail() throws Exception {
        saveEmployee("Sara", "Ahmed", "sara@company.com", Role.EMPLOYEE);

        mockMvc.perform(get(URL)
                        .header("Authorization", "Bearer " + managerToken)
                        .param("search", "sara@company"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[?(@.email == 'sara@company.com')]").isNotEmpty());
    }

    @Test
    void listEmployeesExcludesInactiveByDefault() throws Exception {
        saveEmployee("Ghost", "User", "ghost@company.com", Role.EMPLOYEE, EmployeeStatus.INACTIVE);

        mockMvc.perform(get(URL).header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[?(@.email == 'ghost@company.com')]").isEmpty());
    }

    @Test
    void listEmployeesIncludesInactiveWhenFlagIsTrue() throws Exception {
        saveEmployee("Ghost", "User", "ghost@company.com", Role.EMPLOYEE, EmployeeStatus.INACTIVE);

        mockMvc.perform(get(URL)
                        .header("Authorization", "Bearer " + managerToken)
                        .param("includeInactive", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[?(@.email == 'ghost@company.com')]").isNotEmpty())
                .andExpect(jsonPath("$.data.items[?(@.status == 'INACTIVE')]").isNotEmpty());
    }

    @Test
    void listEmployeesReturnsCorrectFieldsIncludingManager() throws Exception {
        EmployeeEntity manager = saveEmployee("John", "Manager", "john.manager@company.com", Role.MANAGER);

        EmployeeEntity emp = EmployeeEntity.builder()
                .firstName("Sara")
                .lastName("Ahmed")
                .email("sara@company.com")
                .password(passwordEncoder.encode("Pass123!"))
                .role(Role.EMPLOYEE)
                .department("Engineering")
                .position("Frontend Developer")
                .hireDate(java.time.LocalDate.of(2026, 3, 25))
                .manager(manager)
                .build();
        employeeRepository.save(emp);

        mockMvc.perform(get(URL)
                        .header("Authorization", "Bearer " + managerToken)
                        .param("search", "sara@company"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].firstName").value("Sara"))
                .andExpect(jsonPath("$.data.items[0].lastName").value("Ahmed"))
                .andExpect(jsonPath("$.data.items[0].email").value("sara@company.com"))
                .andExpect(jsonPath("$.data.items[0].role").value("EMPLOYEE"))
                .andExpect(jsonPath("$.data.items[0].department").value("Engineering"))
                .andExpect(jsonPath("$.data.items[0].position").value("Frontend Developer"))
                .andExpect(jsonPath("$.data.items[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.items[0].hireDate").value("2026-03-25"))
                .andExpect(jsonPath("$.data.items[0].manager.id").value(manager.getId()))
                .andExpect(jsonPath("$.data.items[0].manager.firstName").value("John"))
                .andExpect(jsonPath("$.data.items[0].manager.lastName").value("Manager"));
    }
}
