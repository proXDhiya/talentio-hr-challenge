package me.dhiya.hr.services.impl;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import me.dhiya.hr.domain.EmployeeEntity;
import me.dhiya.hr.domain.LeaveRequestEntity;
import me.dhiya.hr.domain.enums.EmployeeStatus;
import me.dhiya.hr.domain.enums.LeaveStatus;
import me.dhiya.hr.domain.enums.LeaveType;
import me.dhiya.hr.dto.common.CustomFieldError;
import me.dhiya.hr.dto.employee.response.EmployeeRefDto;
import me.dhiya.hr.domain.enums.Role;
import me.dhiya.hr.dto.leave.request.CreateLeaveRequest;
import me.dhiya.hr.dto.leave.response.LeaveRequestDto;
import org.springframework.web.server.ResponseStatusException;
import me.dhiya.hr.dto.leave.response.LeaveRequestListItemDto;
import me.dhiya.hr.dto.leave.response.LeaveRequestPageDto;
import me.dhiya.hr.dto.leave.response.LeaveReviewDto;
import me.dhiya.hr.exception.BusinessException;
import me.dhiya.hr.repositories.LeaveRequestRepository;
import me.dhiya.hr.repositories.projections.LeaveRequestRow;
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
    public LeaveRequestDto getLeaveRequestById(String id, EmployeeEntity currentUser) {
        LeaveRequestRow r = leaveRequestRepository.findLeaveRequestById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Leave request not found"));

        if (currentUser.getRole() == Role.EMPLOYEE && !currentUser.getId().equals(r.getEmployeeId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied. You can only view your own leave requests.");
        }

        int totalDays = (int) (r.getEndDate().toEpochDay() - r.getStartDate().toEpochDay() + 1);
        return LeaveRequestDto.builder()
                .id(r.getId())
                .employee(r.getEmployeeId() != null ? EmployeeRefDto.builder()
                        .id(r.getEmployeeId())
                        .firstName(r.getEmployeeFirstName())
                        .lastName(r.getEmployeeLastName())
                        .department(r.getEmployeeDepartment())
                        .build() : null)
                .startDate(r.getStartDate())
                .endDate(r.getEndDate())
                .totalDays(totalDays)
                .type(LeaveType.valueOf(r.getType()))
                .status(LeaveStatus.valueOf(r.getStatus()))
                .reason(r.getReason())
                .reviewedBy(r.getReviewedById() != null ? EmployeeRefDto.builder()
                        .id(r.getReviewedById())
                        .firstName(r.getReviewedByFirstName())
                        .lastName(r.getReviewedByLastName())
                        .build() : null)
                .reviewComment(r.getReviewComment())
                .createdAt(r.getCreatedAt().toInstant())
                .updatedAt(r.getUpdatedAt() != null ? r.getUpdatedAt().toInstant() : null)
                .build();
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

    @Override
    @Transactional
    public LeaveReviewDto approveLeaveRequest(String id, String comment, EmployeeEntity currentUser) {
        LeaveRequestEntity leave = findPendingLeave(id, currentUser, "approve");
        leave.setStatus(LeaveStatus.APPROVED);
        leave.setReviewedBy(currentUser);
        leave.setReviewComment(comment);
        leaveRequestRepository.save(leave);
        return toReviewDto(leave);
    }

    @Override
    @Transactional
    public LeaveReviewDto rejectLeaveRequest(String id, String comment, EmployeeEntity currentUser) {
        LeaveRequestEntity leave = findPendingLeave(id, currentUser, "reject");
        leave.setStatus(LeaveStatus.REJECTED);
        leave.setReviewedBy(currentUser);
        leave.setReviewComment(comment);
        leaveRequestRepository.save(leave);
        return toReviewDto(leave);
    }

    private LeaveRequestEntity findPendingLeave(String id, EmployeeEntity currentUser, String action) {
        LeaveRequestEntity leave = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Leave request not found"));

        if (leave.getStatus() != LeaveStatus.PENDING) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Cannot " + action + " a leave request that is already " + leave.getStatus());
        }

        if (currentUser.getRole() == Role.MANAGER) {
            EmployeeEntity manager = leave.getEmployee().getManager();
            if (manager == null || !manager.getId().equals(currentUser.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Access denied. You can only " + action + " requests from your team members.");
            }
        }

        return leave;
    }

    private LeaveReviewDto toReviewDto(LeaveRequestEntity leave) {
        return LeaveReviewDto.builder()
                .id(leave.getId())
                .status(leave.getStatus())
                .reviewedBy(EmployeeRefDto.builder()
                        .id(leave.getReviewedBy().getId())
                        .firstName(leave.getReviewedBy().getFirstName())
                        .lastName(leave.getReviewedBy().getLastName())
                        .build())
                .reviewComment(leave.getReviewComment())
                .updatedAt(leave.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public LeaveRequestPageDto listLeaveRequests(
            String cursor, int size, String employeeId, EmployeeStatus employeeStatus,
            LeaveStatus status, LeaveType type, LocalDate fromDate, LocalDate toDate) {
        List<LeaveRequestRow> results = leaveRequestRepository.findLeaveRequestsPage(
                employeeId,
                employeeStatus.name(),
                status != null ? status.name() : null,
                type != null ? type.name() : null,
                fromDate, toDate, cursor, size + 1
        );

        boolean hasMore = results.size() > size;
        List<LeaveRequestRow> items = hasMore ? results.subList(0, size) : results;
        String nextCursor = hasMore ? items.getLast().getId() : null;

        List<LeaveRequestListItemDto> dtos = items.stream().map(r -> {
            int totalDays = (int) (r.getEndDate().toEpochDay() - r.getStartDate().toEpochDay() + 1);
            return LeaveRequestListItemDto.builder()
                    .id(r.getId())
                    .employee(r.getEmployeeId() != null ? EmployeeRefDto.builder()
                            .id(r.getEmployeeId())
                            .firstName(r.getEmployeeFirstName())
                            .lastName(r.getEmployeeLastName())
                            .department(r.getEmployeeDepartment())
                            .build() : null)
                    .startDate(r.getStartDate())
                    .endDate(r.getEndDate())
                    .totalDays(totalDays)
                    .type(LeaveType.valueOf(r.getType()))
                    .status(LeaveStatus.valueOf(r.getStatus()))
                    .reason(r.getReason())
                    .reviewedBy(r.getReviewedById() != null ? EmployeeRefDto.builder()
                            .id(r.getReviewedById())
                            .firstName(r.getReviewedByFirstName())
                            .lastName(r.getReviewedByLastName())
                            .build() : null)
                    .reviewComment(r.getReviewComment())
                    .createdAt(r.getCreatedAt().toInstant())
                    .build();
        }).toList();

        return LeaveRequestPageDto.builder()
                .items(dtos)
                .nextCursor(nextCursor)
                .hasMore(hasMore)
                .size(size)
                .build();
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
