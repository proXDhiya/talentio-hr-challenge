package me.dhiya.hr.services.impl;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import me.dhiya.hr.domain.EmployeeEntity;
import me.dhiya.hr.domain.LeaveRequestEntity;
import me.dhiya.hr.domain.enums.LeaveStatus;
import me.dhiya.hr.domain.enums.LeaveType;
import me.dhiya.hr.dto.common.CustomFieldError;
import me.dhiya.hr.dto.leave.request.CreateLeaveRequest;
import me.dhiya.hr.exception.BusinessException;
import me.dhiya.hr.repositories.LeaveRequestRepository;
import me.dhiya.hr.services.LeaveRequestService;
import java.time.LocalDate;
import java.util.List;

@Service
public class LeaveRequestServiceImpl implements LeaveRequestService {

    private final LeaveRequestRepository leaveRequestRepository;

    public LeaveRequestServiceImpl(LeaveRequestRepository leaveRequestRepository) {
        this.leaveRequestRepository = leaveRequestRepository;
    }

    @Override
    @Transactional
    public LeaveRequestEntity submitLeaveRequest(CreateLeaveRequest request, EmployeeEntity employee) {
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST,
                    "End date must be after or equal to start date",
                    List.of(CustomFieldError.builder()
                            .field("endDate")
                            .message("End date must be after or equal to start date")
                            .build()));
        }

        int totalDays = (int) (request.getEndDate().toEpochDay() - request.getStartDate().toEpochDay() + 1);

        if (request.getType() == LeaveType.ANNUAL) {
            int usedDays = calculateUsedLeaveDays(employee);
            int remaining = employee.getAnnualLeaveDays() - usedDays;
            if (totalDays > remaining) {
                throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Insufficient leave balance",
                        List.of(CustomFieldError.builder()
                                .field("totalDays")
                                .message("Requested " + totalDays + " days but only " + remaining + " annual leave days remaining")
                                .build()));
            }
        }

        leaveRequestRepository
                .findFirstByEmployee_IdAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        employee.getId(),
                        List.of(LeaveStatus.APPROVED, LeaveStatus.PENDING),
                        request.getEndDate(),
                        request.getStartDate())
                .ifPresent(overlap -> {
                    throw new BusinessException(HttpStatus.CONFLICT,
                            "You already have a leave request overlapping these dates",
                            List.of(CustomFieldError.builder()
                                    .field("startDate")
                                    .message("Overlaps with existing leave request from " + overlap.getStartDate() + " to " + overlap.getEndDate())
                                    .build()));
                });

        return leaveRequestRepository.save(LeaveRequestEntity.builder()
                .employee(employee)
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .type(request.getType())
                .reason(request.getReason())
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    public int calculateUsedLeaveDays(EmployeeEntity employee) {
        LocalDate[] cycle = getCurrentLeaveCycle(employee.getHireDate());
        return leaveRequestRepository
                .findApprovedLeaveDates(employee.getId(), LeaveStatus.APPROVED, cycle[0], cycle[1])
                .stream()
                .mapToInt(r -> (int) (r.getEndDate().toEpochDay() - r.getStartDate().toEpochDay() + 1))
                .sum();
    }

    private LocalDate[] getCurrentLeaveCycle(LocalDate hireDate) {
        LocalDate today = LocalDate.now();
        LocalDate thisYearAnniversary = hireDate.withYear(today.getYear());

        if (today.isBefore(thisYearAnniversary)) {
            return new LocalDate[]{thisYearAnniversary.minusYears(1), thisYearAnniversary.minusDays(1)};
        } else {
            return new LocalDate[]{thisYearAnniversary, thisYearAnniversary.plusYears(1).minusDays(1)};
        }
    }
}
