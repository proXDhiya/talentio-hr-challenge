package me.dhiya.hr.services;

import me.dhiya.hr.domain.EmployeeEntity;
import me.dhiya.hr.domain.LeaveRequestEntity;
import me.dhiya.hr.domain.enums.EmployeeStatus;
import me.dhiya.hr.domain.enums.LeaveStatus;
import me.dhiya.hr.domain.enums.LeaveType;
import me.dhiya.hr.dto.leave.request.CreateLeaveRequest;
import me.dhiya.hr.dto.leave.response.LeaveRequestPageDto;
import java.time.LocalDate;

public interface LeaveRequestService {
    LeaveRequestEntity submitLeaveRequest(CreateLeaveRequest request, EmployeeEntity employee);
    int calculateUsedLeaveDays(EmployeeEntity employee);
    LeaveRequestPageDto listLeaveRequests(String cursor, int size, String employeeId, EmployeeStatus employeeStatus, LeaveStatus status, LeaveType type, LocalDate fromDate, LocalDate toDate);
}
