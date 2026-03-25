package me.dhiya.hr.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.server.ResponseStatusException;
import me.dhiya.hr.TestDataUtil;
import me.dhiya.hr.domain.EmployeeEntity;
import me.dhiya.hr.domain.LeaveRequestEntity;
import me.dhiya.hr.domain.enums.LeaveStatus;
import me.dhiya.hr.domain.enums.LeaveType;
import me.dhiya.hr.domain.enums.Role;
import me.dhiya.hr.dto.leave.request.CreateLeaveRequest;
import me.dhiya.hr.repositories.EmployeeRepository;
import me.dhiya.hr.repositories.LeaveRequestRepository;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class LeaveRequestServiceTests {

    @Autowired private LeaveRequestService leaveRequestService;
    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private LeaveRequestRepository leaveRequestRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Test
    void submitLeaveRequestSavesWithCorrectFields() {
        EmployeeEntity employee = savedEmployee();
        CreateLeaveRequest request = buildRequest(LeaveType.ANNUAL,
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(5), "Vacation");

        LeaveRequestEntity result = leaveRequestService.submitLeaveRequest(request, employee);

        assertThat(result.getId()).isNotNull();
        assertThat(result.getEmployee().getId()).isEqualTo(employee.getId());
        assertThat(result.getStartDate()).isEqualTo(LocalDate.now().plusDays(1));
        assertThat(result.getEndDate()).isEqualTo(LocalDate.now().plusDays(5));
        assertThat(result.getType()).isEqualTo(LeaveType.ANNUAL);
        assertThat(result.getReason()).isEqualTo("Vacation");
        assertThat(result.getCreatedAt()).isNotNull();
    }

    @Test
    void submitLeaveRequestDefaultsStatusToPending() {
        EmployeeEntity employee = savedEmployee();
        CreateLeaveRequest request = buildRequest(LeaveType.ANNUAL,
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(3), null);

        LeaveRequestEntity result = leaveRequestService.submitLeaveRequest(request, employee);

        assertThat(result.getStatus()).isEqualTo(LeaveStatus.PENDING);
    }

    @Test
    void submitLeaveRequestThrows400WhenEndDateBeforeStartDate() {
        EmployeeEntity employee = savedEmployee();
        CreateLeaveRequest request = buildRequest(LeaveType.ANNUAL,
                LocalDate.now().plusDays(5), LocalDate.now().plusDays(1), null);

        assertThatThrownBy(() -> leaveRequestService.submitLeaveRequest(request, employee))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("End date must be after or equal to start date");
    }

    @Test
    void submitLeaveRequestThrows422WhenInsufficientAnnualBalance() {
        EmployeeEntity employee = savedEmployee(2);
        CreateLeaveRequest request = buildRequest(LeaveType.ANNUAL,
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(5), null);

        assertThatThrownBy(() -> leaveRequestService.submitLeaveRequest(request, employee))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Insufficient leave balance");
    }

    @Test
    void submitLeaveRequestDoesNotCheckBalanceForSickLeave() {
        EmployeeEntity employee = savedEmployee(0);
        CreateLeaveRequest request = buildRequest(LeaveType.SICK,
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(5), null);

        LeaveRequestEntity result = leaveRequestService.submitLeaveRequest(request, employee);

        assertThat(result.getId()).isNotNull();
    }

    @Test
    void submitLeaveRequestDoesNotCheckBalanceForUnpaidLeave() {
        EmployeeEntity employee = savedEmployee(0);
        CreateLeaveRequest request = buildRequest(LeaveType.UNPAID,
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(5), null);

        LeaveRequestEntity result = leaveRequestService.submitLeaveRequest(request, employee);

        assertThat(result.getId()).isNotNull();
    }

    @Test
    void submitLeaveRequestThrows409WhenOverlapsWithApprovedLeave() {
        EmployeeEntity employee = savedEmployee();
        saveLeave(employee, LocalDate.now().plusDays(3), LocalDate.now().plusDays(7), LeaveStatus.APPROVED);

        CreateLeaveRequest request = buildRequest(LeaveType.ANNUAL,
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(5), null);

        assertThatThrownBy(() -> leaveRequestService.submitLeaveRequest(request, employee))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("You already have a leave request overlapping these dates");
    }

    @Test
    void submitLeaveRequestThrows409WhenOverlapsWithPendingLeave() {
        EmployeeEntity employee = savedEmployee();
        saveLeave(employee, LocalDate.now().plusDays(3), LocalDate.now().plusDays(7), LeaveStatus.PENDING);

        CreateLeaveRequest request = buildRequest(LeaveType.ANNUAL,
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(5), null);

        assertThatThrownBy(() -> leaveRequestService.submitLeaveRequest(request, employee))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("You already have a leave request overlapping these dates");
    }

    @Test
    void submitLeaveRequestDoesNotThrowForRejectedOverlappingLeave() {
        EmployeeEntity employee = savedEmployee();
        saveLeave(employee, LocalDate.now().plusDays(3), LocalDate.now().plusDays(7), LeaveStatus.REJECTED);

        CreateLeaveRequest request = buildRequest(LeaveType.ANNUAL,
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(5), null);

        LeaveRequestEntity result = leaveRequestService.submitLeaveRequest(request, employee);

        assertThat(result.getId()).isNotNull();
    }

    @Test
    void calculateUsedLeaveDaysReturnsZeroWhenNoLeaves() {
        EmployeeEntity employee = savedEmployee(LocalDate.of(2020, 1, 15));

        assertThat(leaveRequestService.calculateUsedLeaveDays(employee)).isZero();
    }

    @Test
    void calculateUsedLeaveDaysCountsApprovedLeavesInCurrentCycle() {
        EmployeeEntity employee = savedEmployee(LocalDate.of(2020, 3, 1));
        LocalDate cycleStart = LocalDate.now().withDayOfMonth(1).withMonth(3);
        if (LocalDate.now().isBefore(cycleStart)) cycleStart = cycleStart.minusYears(1);

        saveLeave(employee, cycleStart.plusDays(5), cycleStart.plusDays(9), LeaveStatus.APPROVED);

        assertThat(leaveRequestService.calculateUsedLeaveDays(employee)).isEqualTo(5);
    }

    @Test
    void calculateUsedLeaveDaysIgnoresNonApprovedLeaves() {
        EmployeeEntity employee = savedEmployee(LocalDate.of(2020, 3, 1));
        LocalDate cycleStart = LocalDate.now().withDayOfMonth(1).withMonth(3);
        if (LocalDate.now().isBefore(cycleStart)) cycleStart = cycleStart.minusYears(1);

        saveLeave(employee, cycleStart.plusDays(5), cycleStart.plusDays(9), LeaveStatus.PENDING);
        saveLeave(employee, cycleStart.plusDays(10), cycleStart.plusDays(14), LeaveStatus.REJECTED);

        assertThat(leaveRequestService.calculateUsedLeaveDays(employee)).isZero();
    }

    @Test
    void calculateUsedLeaveDaysIgnoresLeavesOutsideCurrentCycle() {
        EmployeeEntity employee = savedEmployee(LocalDate.of(2020, 3, 1));
        saveLeave(employee, LocalDate.now().minusYears(5), LocalDate.now().minusYears(5).plusDays(4), LeaveStatus.APPROVED);

        assertThat(leaveRequestService.calculateUsedLeaveDays(employee)).isZero();
    }

    @Test
    void calculateUsedLeaveDaysSumsMultipleApprovedLeaves() {
        EmployeeEntity employee = savedEmployee(LocalDate.of(2020, 3, 1));
        LocalDate cycleStart = LocalDate.now().withDayOfMonth(1).withMonth(3);
        if (LocalDate.now().isBefore(cycleStart)) cycleStart = cycleStart.minusYears(1);

        saveLeave(employee, cycleStart.plusDays(5), cycleStart.plusDays(6), LeaveStatus.APPROVED);
        saveLeave(employee, cycleStart.plusDays(10), cycleStart.plusDays(12), LeaveStatus.APPROVED);

        assertThat(leaveRequestService.calculateUsedLeaveDays(employee)).isEqualTo(5);
    }

    private EmployeeEntity savedEmployee() {
        return savedEmployee(30);
    }

    private EmployeeEntity savedEmployee(int annualLeaveDays) {
        EmployeeEntity employee = TestDataUtil.createEmployee();
        employee.setPassword(passwordEncoder.encode(employee.getPassword()));
        employee.setRole(Role.EMPLOYEE);
        employee.setHireDate(LocalDate.now().minusYears(1));
        employee.setAnnualLeaveDays(annualLeaveDays);
        return employeeRepository.save(employee);
    }

    private EmployeeEntity savedEmployee(LocalDate hireDate) {
        EmployeeEntity employee = TestDataUtil.createEmployee();
        employee.setPassword(passwordEncoder.encode(employee.getPassword()));
        employee.setRole(Role.EMPLOYEE);
        employee.setHireDate(hireDate);
        employee.setAnnualLeaveDays(30);
        return employeeRepository.save(employee);
    }

    private void saveLeave(EmployeeEntity employee, LocalDate start, LocalDate end, LeaveStatus status) {
        leaveRequestRepository.save(LeaveRequestEntity.builder()
                .employee(employee)
                .startDate(start)
                .endDate(end)
                .type(LeaveType.ANNUAL)
                .status(status)
                .build());
    }

    private CreateLeaveRequest buildRequest(LeaveType type, LocalDate start, LocalDate end, String reason) {
        return CreateLeaveRequest.builder()
                .type(type)
                .startDate(start)
                .endDate(end)
                .reason(reason)
                .build();
    }
}
