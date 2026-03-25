package me.dhiya.hr.controllers.leave;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import me.dhiya.hr.controllers.BaseControllerTest;
import me.dhiya.hr.domain.EmployeeEntity;
import me.dhiya.hr.domain.LeaveRequestEntity;
import me.dhiya.hr.domain.enums.LeaveStatus;
import me.dhiya.hr.domain.enums.LeaveType;
import me.dhiya.hr.domain.enums.Role;
import me.dhiya.hr.repositories.LeaveRequestRepository;
import java.time.LocalDate;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class LeaveRequestCreateTests extends BaseControllerTest {

    private static final String URL = "/apis/v1/leave-requests";

    @Autowired private LeaveRequestRepository leaveRequestRepository;

    private String employeeToken;
    private EmployeeEntity savedEmployee;

    @BeforeEach
    void createUsers() {
        savedEmployee = saveEmployee("Sara", "Ahmed", "sara@company.com", Role.EMPLOYEE);
        employeeToken = token(savedEmployee);
    }

    private String validRequest() {
        return """
                {
                  "startDate": "%s",
                  "endDate": "%s",
                  "type": "ANNUAL",
                  "reason": "Family vacation"
                }
                """.formatted(LocalDate.now().plusDays(1), LocalDate.now().plusDays(5));
    }

    @Test
    void submitLeaveRequestReturns401WhenNoTokenProvided() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void submitLeaveRequestReturns201WhenCalledByEmployee() throws Exception {
        mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Leave request submitted successfully"))
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andExpect(jsonPath("$.data.employee.id").value(savedEmployee.getId()))
                .andExpect(jsonPath("$.data.employee.firstName").value("Sara"))
                .andExpect(jsonPath("$.data.employee.lastName").value("Ahmed"))
                .andExpect(jsonPath("$.data.startDate").value(LocalDate.now().plusDays(1).toString()))
                .andExpect(jsonPath("$.data.endDate").value(LocalDate.now().plusDays(5).toString()))
                .andExpect(jsonPath("$.data.totalDays").value(5))
                .andExpect(jsonPath("$.data.type").value("ANNUAL"))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.reason").value("Family vacation"))
                .andExpect(jsonPath("$.data.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void submitLeaveRequestReturns201WhenCalledByManager() throws Exception {
        String managerToken = token(saveEmployee("John", "Doe", "manager@company.com", Role.MANAGER));

        mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer " + managerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isCreated());
    }

    @Test
    void submitLeaveRequestReturns201WhenCalledByHr() throws Exception {
        String hrToken = token(saveEmployee("Jane", "Smith", "hr@company.com", Role.HR));

        mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer " + hrToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isCreated());
    }

    @Test
    void submitLeaveRequestReturns400WhenStartDateIsInThePast() throws Exception {
        mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"startDate": "%s", "endDate": "%s", "type": "ANNUAL"}
                                """.formatted(LocalDate.now().minusDays(1), LocalDate.now().plusDays(5))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors[0].field").value("startDate"));
    }

    @Test
    void submitLeaveRequestReturns400WhenStartDateIsMissing() throws Exception {
        mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"endDate": "%s", "type": "ANNUAL"}
                                """.formatted(LocalDate.now().plusDays(5))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("startDate"));
    }

    @Test
    void submitLeaveRequestReturns400WhenEndDateIsMissing() throws Exception {
        mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"startDate": "%s", "type": "ANNUAL"}
                                """.formatted(LocalDate.now().plusDays(1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("endDate"));
    }

    @Test
    void submitLeaveRequestReturns400WhenTypeIsMissing() throws Exception {
        mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"startDate": "%s", "endDate": "%s"}
                                """.formatted(LocalDate.now().plusDays(1), LocalDate.now().plusDays(5))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("type"));
    }

    @Test
    void submitLeaveRequestReturns400WhenTypeIsInvalid() throws Exception {
        mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"startDate": "%s", "endDate": "%s", "type": "INVALID"}
                                """.formatted(LocalDate.now().plusDays(1), LocalDate.now().plusDays(5))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void submitLeaveRequestReturns400WhenEndDateIsBeforeStartDate() throws Exception {
        mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"startDate": "%s", "endDate": "%s", "type": "ANNUAL"}
                                """.formatted(LocalDate.now().plusDays(5), LocalDate.now().plusDays(1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("endDate"));
    }

    @Test
    void submitLeaveRequestReturns400WhenReasonExceeds500Chars() throws Exception {
        mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"startDate": "%s", "endDate": "%s", "type": "ANNUAL", "reason": "%s"}
                                """.formatted(LocalDate.now().plusDays(1), LocalDate.now().plusDays(5), "a".repeat(501))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("reason"));
    }

    @Test
    void submitLeaveRequestReturns422WhenInsufficientAnnualLeaveBalance() throws Exception {
        EmployeeEntity lowBalanceEmployee = employeeRepository.findById(savedEmployee.getId()).orElseThrow();
        lowBalanceEmployee.setAnnualLeaveDays(2);
        employeeRepository.save(lowBalanceEmployee);

        mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"startDate": "%s", "endDate": "%s", "type": "ANNUAL"}
                                """.formatted(LocalDate.now().plusDays(1), LocalDate.now().plusDays(5))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Insufficient leave balance"))
                .andExpect(jsonPath("$.errors[0].field").value("totalDays"));
    }

    @Test
    void submitLeaveRequestReturns409WhenOverlappingLeaveExists() throws Exception {
        leaveRequestRepository.save(LeaveRequestEntity.builder()
                .employee(savedEmployee)
                .startDate(LocalDate.now().plusDays(3))
                .endDate(LocalDate.now().plusDays(7))
                .type(LeaveType.ANNUAL)
                .status(LeaveStatus.APPROVED)
                .build());

        mockMvc.perform(post(URL)
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validRequest()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("You already have a leave request overlapping these dates"))
                .andExpect(jsonPath("$.errors[0].field").value("startDate"));
    }
}
