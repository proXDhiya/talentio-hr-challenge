package me.dhiya.hr.controllers.leave;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import me.dhiya.hr.controllers.BaseControllerTest;
import me.dhiya.hr.domain.EmployeeEntity;
import me.dhiya.hr.domain.LeaveRequestEntity;
import me.dhiya.hr.domain.enums.LeaveStatus;
import me.dhiya.hr.domain.enums.LeaveType;
import me.dhiya.hr.domain.enums.Role;
import java.time.LocalDate;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class LeaveRequestApproveTests extends BaseControllerTest {

    private static final String URL = "/apis/v1/leave-requests/{id}/approve";

    private EmployeeEntity manager;
    private EmployeeEntity employee;
    private String managerToken;
    private String hrToken;
    private String employeeToken;
    private LeaveRequestEntity leave;

    @BeforeEach
    void setup() {
        manager = saveEmployee("John", "Doe", "manager@company.com", Role.MANAGER);
        managerToken = token(manager);
        hrToken = token(saveEmployee("Jane", "Smith", "hr@company.com", Role.HR));
        employee = saveEmployeeWithManager("Bob", "Jones", "bob@company.com", manager);
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
    void approveReturns401WhenNoTokenProvided() throws Exception {
        mockMvc.perform(patch(URL, leave.getId())
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void approveReturns403WhenCalledByEmployee() throws Exception {
        mockMvc.perform(patch(URL, leave.getId())
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void approveReturns200WhenCalledByManager() throws Exception {
        mockMvc.perform(patch(URL, leave.getId())
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"Approved!\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Leave request approved successfully"))
                .andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.reviewedBy.id").value(manager.getId()))
                .andExpect(jsonPath("$.data.reviewComment").value("Approved!"))
                .andExpect(jsonPath("$.data.updatedAt").isNotEmpty());
    }

    @Test
    void approveReturns200WhenCalledByHr() throws Exception {
        mockMvc.perform(patch(URL, leave.getId())
                        .header("Authorization", "Bearer " + hrToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));
    }

    @Test
    void approveReturns200WithoutComment() throws Exception {
        mockMvc.perform(patch(URL, leave.getId())
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));
    }

    @Test
    void approveReturns404WhenLeaveNotFound() throws Exception {
        mockMvc.perform(patch(URL, "00000000-0000-0000-0000-000000000000")
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Leave request not found"));
    }

    @Test
    void approveReturns422WhenLeaveAlreadyApproved() throws Exception {
        leave.setStatus(LeaveStatus.APPROVED);
        leaveRequestRepository.save(leave);

        mockMvc.perform(patch(URL, leave.getId())
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Cannot approve a leave request that is already APPROVED"));
    }

    @Test
    void approveReturns403WhenManagerApprovesOutsideTeam() throws Exception {
        EmployeeEntity otherEmployee = saveEmployee("Sara", "Ahmed", "sara@company.com", Role.EMPLOYEE);
        LeaveRequestEntity otherLeave = leaveRequestRepository.save(LeaveRequestEntity.builder()
                .employee(otherEmployee)
                .startDate(LocalDate.now().plusDays(1))
                .endDate(LocalDate.now().plusDays(3))
                .type(LeaveType.ANNUAL)
                .status(LeaveStatus.PENDING)
                .build());

        mockMvc.perform(patch(URL, otherLeave.getId())
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied. You can only approve requests from your team members."));
    }
}
