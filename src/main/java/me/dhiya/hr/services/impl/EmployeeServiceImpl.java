package me.dhiya.hr.services.impl;

import me.dhiya.hr.domain.EmployeeEntity;
import me.dhiya.hr.domain.enums.LeaveStatus;
import me.dhiya.hr.domain.enums.Role;
import me.dhiya.hr.repositories.EmployeeRepository;
import me.dhiya.hr.repositories.LeaveRequestRepository;
import me.dhiya.hr.services.EmployeeService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Optional;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final PasswordEncoder passwordEncoder;

    public EmployeeServiceImpl(EmployeeRepository employeeRepository,
                               LeaveRequestRepository leaveRequestRepository,
                               PasswordEncoder passwordEncoder) {
        this.employeeRepository = employeeRepository;
        this.leaveRequestRepository = leaveRequestRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EmployeeEntity> authenticate(String email, String password) {
        return employeeRepository.findByEmail(email)
                .filter(employee -> passwordEncoder.matches(password, employee.getPassword()));
    }

    @Override
    public EmployeeEntity setup(EmployeeEntity employee) {
        if (employeeRepository.count() > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Setup has already been completed");
        }
        employee.setPassword(passwordEncoder.encode(employee.getPassword()));
        employee.setRole(Role.MANAGER);
        employee.setHireDate(LocalDate.now());
        return employeeRepository.save(employee);
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeEntity getProfile(String employeeId) {
        return employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));
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
