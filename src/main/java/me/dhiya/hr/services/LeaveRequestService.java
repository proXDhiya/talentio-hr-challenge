package me.dhiya.hr.services;

import me.dhiya.hr.domain.EmployeeEntity;
import me.dhiya.hr.domain.LeaveRequestEntity;
import me.dhiya.hr.dto.leave.request.CreateLeaveRequest;

public interface LeaveRequestService {
    LeaveRequestEntity submitLeaveRequest(CreateLeaveRequest request, EmployeeEntity employee);
    int calculateUsedLeaveDays(EmployeeEntity employee);
}
