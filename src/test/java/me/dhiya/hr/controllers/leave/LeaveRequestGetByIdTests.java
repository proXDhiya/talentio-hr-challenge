package me.dhiya.hr.controllers.leave;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import me.dhiya.hr.controllers.BaseControllerTest;
import me.dhiya.hr.domain.EmployeeEntity;
import me.dhiya.hr.domain.LeaveRequestEntity;
import me.dhiya.hr.domain.enums.LeaveStatus;
import me.dhiya.hr.domain.enums.LeaveType;
import me.dhiya.hr.domain.enums.Role;
import java.time.LocalDate;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class LeaveRequestGetByIdTests extends BaseControllerTest {

    private static final String URL = "/apis/v1/leave-requests/{id}";

    private String managerToken;
    private String hrToken;
    private String employeeToken;
    private EmployeeEntity employee;
    private LeaveRequestEntity leave;

    @BeforeEach
    void setup() {
        EmployeeEntity manager = saveEmployee("John", "Doe", "manager@company.com", Role.MANAGER);
        managerToken = token(manager);
        hrToken = token(saveEmployee("Jane", "Smith", "hr@company.com", Role.HR));
        employee = saveEmployee("Bob", "Jones", "bob@company.com", Role.EMPLOYEE);
        employeeToken = token(employee);
        leave = leaveRequestRepository.save(LeaveRequestEntity.builder()
                .employee(employee)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(5))
                .type(LeaveType.ANNUAL)
                .status(LeaveStatus.PENDING)
                .reason("Vacation")
                .build());
    }

    @Test
    void getLeaveRequestByIdReturns401WhenNoTokenProvided() throws Exception {
        mockMvc.perform(get(URL, leave.getId()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getLeaveRequestByIdReturns200WhenCalledByManager() throws Exception {
        mockMvc.perform(get(URL, leave.getId()).header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Leave request retrieved successfully"));
    }

    @Test
    void getLeaveRequestByIdReturns200WhenCalledByHr() throws Exception {
        mockMvc.perform(get(URL, leave.getId()).header("Authorization", "Bearer " + hrToken))
                .andExpect(status().isOk());
    }

    @Test
    void getLeaveRequestByIdReturns200WhenEmployeeViewsOwnLeave() throws Exception {
        mockMvc.perform(get(URL, leave.getId()).header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk());
    }

    @Test
    void getLeaveRequestByIdReturns403WhenEmployeeViewsOthersLeave() throws Exception {
        EmployeeEntity other = saveEmployee("Sara", "Ahmed", "sara@company.com", Role.EMPLOYEE);
        String otherToken = token(other);

        mockMvc.perform(get(URL, leave.getId()).header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied. You can only view your own leave requests."));
    }

    @Test
    void getLeaveRequestByIdReturns404WhenNotFound() throws Exception {
        mockMvc.perform(get(URL, "00000000-0000-0000-0000-000000000000")
                        .header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Leave request not found"));
    }

    @Test
    void getLeaveRequestByIdReturnsAllFields() throws Exception {
        mockMvc.perform(get(URL, leave.getId()).header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(leave.getId()))
                .andExpect(jsonPath("$.data.employee.id").value(employee.getId()))
                .andExpect(jsonPath("$.data.employee.firstName").value("Bob"))
                .andExpect(jsonPath("$.data.employee.lastName").value("Jones"))
                .andExpect(jsonPath("$.data.startDate").value(LocalDate.now().plusDays(1).toString()))
                .andExpect(jsonPath("$.data.endDate").value(LocalDate.now().plusDays(5).toString()))
                .andExpect(jsonPath("$.data.totalDays").value(5))
                .andExpect(jsonPath("$.data.type").value("ANNUAL"))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.reason").value("Vacation"))
                .andExpect(jsonPath("$.data.createdAt").isNotEmpty());
    }

    @Test
    void getLeaveRequestByIdReturnsNullReviewedByWhenNotReviewed() throws Exception {
        mockMvc.perform(get(URL, leave.getId()).header("Authorization", "Bearer " + managerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reviewedBy").doesNotExist())
                .andExpect(jsonPath("$.data.reviewComment").doesNotExist());
    }
}
