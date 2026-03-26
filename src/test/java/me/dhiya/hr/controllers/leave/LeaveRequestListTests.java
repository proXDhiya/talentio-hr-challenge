package me.dhiya.hr.controllers.leave;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MvcResult;
import me.dhiya.hr.controllers.BaseControllerTest;
import me.dhiya.hr.domain.EmployeeEntity;
import me.dhiya.hr.domain.LeaveRequestEntity;
import me.dhiya.hr.domain.enums.LeaveStatus;
import me.dhiya.hr.domain.enums.LeaveType;
import me.dhiya.hr.domain.enums.Role;
import me.dhiya.hr.repositories.LeaveRequestRepository;
import java.time.LocalDate;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class LeaveRequestListTests extends BaseControllerTest {

    private static final String URL = "/apis/v1/leave-requests";

    @Autowired private LeaveRequestRepository leaveRequestRepository;

    private String managerToken;
    private String hrToken;
    private String employeeToken;
    private EmployeeEntity savedEmployee;

    @BeforeEach
    void createUsers() {
        managerToken = token(saveEmployee("John", "Doe", "manager@company.com", Role.MANAGER));
        hrToken = token(saveEmployee("Jane", "Smith", "hr@company.com", Role.HR));
        savedEmployee = saveEmployee("Sara", "Ahmed", "sara@company.com", "Engineering", Role.EMPLOYEE, null);
        employeeToken = token(savedEmployee);
    }

    private LeaveRequestEntity saveLeave(EmployeeEntity employee, LocalDate start, LocalDate end, LeaveStatus status, LeaveType type) {
        return leaveRequestRepository.save(LeaveRequestEntity.builder()
                .employee(employee)
                .startDate(start)
                .endDate(end)
                .type(type)
                .status(status)
                .build());
    }

    @Test
    void listLeaveRequestsReturns401WhenNoTokenProvided() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listLeaveRequestsReturns200WhenCalledByManager() throws Exception {
        mockMvc.perform(get(URL).header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Leave requests retrieved successfully"))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.hasMore").isBoolean())
                .andExpect(jsonPath("$.data.size").isNumber())
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void listLeaveRequestsReturns200WhenCalledByHr() throws Exception {
        mockMvc.perform(get(URL).header("Authorization", "Bearer " + hrToken))
                .andExpect(status().isOk());
    }

    @Test
    void listLeaveRequestsReturns200WhenCalledByEmployee() throws Exception {
        mockMvc.perform(get(URL).header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk());
    }

    @Test
    void listLeaveRequestsReturnsDefaultSizeOf20() throws Exception {
        mockMvc.perform(get(URL).header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.size").value(20));
    }

    @Test
    void listLeaveRequestsReturnsRequestedSize() throws Exception {
        mockMvc.perform(get(URL)
                        .header("Authorization", "Bearer " + managerToken)
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.size").value(5));
    }

    @Test
    void listLeaveRequestsReturns400WhenSizeExceedsMax() throws Exception {
        mockMvc.perform(get(URL)
                        .header("Authorization", "Bearer " + managerToken)
                        .param("size", "200"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors[0].field").value("size"));
    }

    @Test
    void listLeaveRequestsReturns400WhenSizeIsBelowMin() throws Exception {
        mockMvc.perform(get(URL)
                        .header("Authorization", "Bearer " + managerToken)
                        .param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("size"));
    }

    @Test
    void listLeaveRequestsReturns400WhenCursorIsNotUUID() throws Exception {
        mockMvc.perform(get(URL)
                        .header("Authorization", "Bearer " + managerToken)
                        .param("cursor", "not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("cursor"));
    }

    @Test
    void listLeaveRequestsReturns400WhenEmployeeIdIsNotUUID() throws Exception {
        mockMvc.perform(get(URL)
                        .header("Authorization", "Bearer " + managerToken)
                        .param("employeeId", "not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("employeeId"));
    }

    @Test
    void listLeaveRequestsReturns400WhenStatusIsInvalid() throws Exception {
        mockMvc.perform(get(URL)
                        .header("Authorization", "Bearer " + managerToken)
                        .param("status", "INVALID_STATUS"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("status"));
    }

    @Test
    void listLeaveRequestsReturns400WhenTypeIsInvalid() throws Exception {
        mockMvc.perform(get(URL)
                        .header("Authorization", "Bearer " + managerToken)
                        .param("type", "INVALID_TYPE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("type"));
    }

    @Test
    void listLeaveRequestsReturnsHasMoreTrueAndNextCursorWhenMorePagesExist() throws Exception {
        EmployeeEntity emp2 = saveEmployee("Bob", "Jones", "bob@company.com", Role.EMPLOYEE);
        EmployeeEntity emp3 = saveEmployee("Ali", "Hassan", "ali@company.com", Role.EMPLOYEE);
        saveLeave(savedEmployee, LocalDate.now().plusDays(1), LocalDate.now().plusDays(3), LeaveStatus.PENDING, LeaveType.ANNUAL);
        saveLeave(emp2, LocalDate.now().plusDays(1), LocalDate.now().plusDays(3), LeaveStatus.PENDING, LeaveType.ANNUAL);
        saveLeave(emp3, LocalDate.now().plusDays(1), LocalDate.now().plusDays(3), LeaveStatus.PENDING, LeaveType.ANNUAL);

        mockMvc.perform(get(URL)
                        .header("Authorization", "Bearer " + managerToken)
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hasMore").value(true))
                .andExpect(jsonPath("$.data.nextCursor").isNotEmpty())
                .andExpect(jsonPath("$.data.items.length()").value(2));
    }

    @Test
    void listLeaveRequestsReturnsHasMoreFalseOnLastPage() throws Exception {
        mockMvc.perform(get(URL)
                        .header("Authorization", "Bearer " + managerToken)
                        .param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hasMore").value(false))
                .andExpect(jsonPath("$.data.nextCursor").doesNotExist());
    }

    @Test
    void listLeaveRequestsReturnsDifferentItemsOnNextPage() throws Exception {
        EmployeeEntity emp2 = saveEmployee("Bob", "Jones", "bob@company.com", Role.EMPLOYEE);
        EmployeeEntity emp3 = saveEmployee("Ali", "Hassan", "ali@company.com", Role.EMPLOYEE);
        EmployeeEntity emp4 = saveEmployee("Mia", "Lee", "mia@company.com", Role.EMPLOYEE);
        saveLeave(savedEmployee, LocalDate.now().plusDays(1), LocalDate.now().plusDays(2), LeaveStatus.PENDING, LeaveType.ANNUAL);
        saveLeave(emp2, LocalDate.now().plusDays(1), LocalDate.now().plusDays(2), LeaveStatus.PENDING, LeaveType.ANNUAL);
        saveLeave(emp3, LocalDate.now().plusDays(1), LocalDate.now().plusDays(2), LeaveStatus.PENDING, LeaveType.ANNUAL);
        saveLeave(emp4, LocalDate.now().plusDays(1), LocalDate.now().plusDays(2), LeaveStatus.PENDING, LeaveType.ANNUAL);

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
    void listLeaveRequestsFiltersByEmployeeId() throws Exception {
        EmployeeEntity other = saveEmployee("Bob", "Jones", "bob@company.com", Role.EMPLOYEE);
        saveLeave(savedEmployee, LocalDate.now().plusDays(1), LocalDate.now().plusDays(3), LeaveStatus.PENDING, LeaveType.ANNUAL);
        saveLeave(other, LocalDate.now().plusDays(1), LocalDate.now().plusDays(3), LeaveStatus.PENDING, LeaveType.ANNUAL);

        mockMvc.perform(get(URL)
                        .header("Authorization", "Bearer " + managerToken)
                        .param("employeeId", savedEmployee.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[?(@.employee.id == '" + other.getId() + "')]").isEmpty())
                .andExpect(jsonPath("$.data.items[?(@.employee.id == '" + savedEmployee.getId() + "')]").isNotEmpty());
    }

    @Test
    void listLeaveRequestsFiltersByStatus() throws Exception {
        saveLeave(savedEmployee, LocalDate.now().plusDays(1), LocalDate.now().plusDays(3), LeaveStatus.PENDING, LeaveType.ANNUAL);
        saveLeave(savedEmployee, LocalDate.now().plusDays(5), LocalDate.now().plusDays(7), LeaveStatus.APPROVED, LeaveType.ANNUAL);

        mockMvc.perform(get(URL)
                        .header("Authorization", "Bearer " + managerToken)
                        .param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[?(@.status == 'APPROVED')]").isEmpty())
                .andExpect(jsonPath("$.data.items[?(@.status == 'PENDING')]").isNotEmpty());
    }

    @Test
    void listLeaveRequestsFiltersByType() throws Exception {
        saveLeave(savedEmployee, LocalDate.now().plusDays(1), LocalDate.now().plusDays(3), LeaveStatus.PENDING, LeaveType.ANNUAL);
        saveLeave(savedEmployee, LocalDate.now().plusDays(5), LocalDate.now().plusDays(7), LeaveStatus.PENDING, LeaveType.SICK);

        mockMvc.perform(get(URL)
                        .header("Authorization", "Bearer " + managerToken)
                        .param("type", "SICK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[?(@.type == 'ANNUAL')]").isEmpty())
                .andExpect(jsonPath("$.data.items[?(@.type == 'SICK')]").isNotEmpty());
    }

    @Test
    void listLeaveRequestsFiltersByFromDate() throws Exception {
        saveLeave(savedEmployee, LocalDate.now().plusDays(1), LocalDate.now().plusDays(3), LeaveStatus.PENDING, LeaveType.ANNUAL);
        saveLeave(savedEmployee, LocalDate.now().plusDays(10), LocalDate.now().plusDays(12), LeaveStatus.PENDING, LeaveType.ANNUAL);

        mockMvc.perform(get(URL)
                        .header("Authorization", "Bearer " + managerToken)
                        .param("fromDate", LocalDate.now().plusDays(8).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[?(@.startDate == '" + LocalDate.now().plusDays(1) + "')]").isEmpty())
                .andExpect(jsonPath("$.data.items[?(@.startDate == '" + LocalDate.now().plusDays(10) + "')]").isNotEmpty());
    }

    @Test
    void listLeaveRequestsFiltersByToDate() throws Exception {
        saveLeave(savedEmployee, LocalDate.now().plusDays(1), LocalDate.now().plusDays(3), LeaveStatus.PENDING, LeaveType.ANNUAL);
        saveLeave(savedEmployee, LocalDate.now().plusDays(10), LocalDate.now().plusDays(12), LeaveStatus.PENDING, LeaveType.ANNUAL);

        mockMvc.perform(get(URL)
                        .header("Authorization", "Bearer " + managerToken)
                        .param("toDate", LocalDate.now().plusDays(5).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[?(@.endDate == '" + LocalDate.now().plusDays(12) + "')]").isEmpty())
                .andExpect(jsonPath("$.data.items[?(@.endDate == '" + LocalDate.now().plusDays(3) + "')]").isNotEmpty());
    }

    @Test
    void listLeaveRequestsReturnsCorrectFields() throws Exception {
        saveLeave(savedEmployee, LocalDate.now().plusDays(1), LocalDate.now().plusDays(5), LeaveStatus.PENDING, LeaveType.ANNUAL);

        mockMvc.perform(get(URL)
                        .header("Authorization", "Bearer " + managerToken)
                        .param("employeeId", savedEmployee.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].id").isNotEmpty())
                .andExpect(jsonPath("$.data.items[0].employee.id").value(savedEmployee.getId()))
                .andExpect(jsonPath("$.data.items[0].employee.firstName").value("Sara"))
                .andExpect(jsonPath("$.data.items[0].employee.lastName").value("Ahmed"))
                .andExpect(jsonPath("$.data.items[0].employee.department").value("Engineering"))
                .andExpect(jsonPath("$.data.items[0].startDate").value(LocalDate.now().plusDays(1).toString()))
                .andExpect(jsonPath("$.data.items[0].endDate").value(LocalDate.now().plusDays(5).toString()))
                .andExpect(jsonPath("$.data.items[0].totalDays").value(5))
                .andExpect(jsonPath("$.data.items[0].type").value("ANNUAL"))
                .andExpect(jsonPath("$.data.items[0].status").value("PENDING"))
                .andExpect(jsonPath("$.data.items[0].createdAt").isNotEmpty());
    }
}
