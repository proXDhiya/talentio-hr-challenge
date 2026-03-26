package me.dhiya.hr.services.impl;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import me.dhiya.hr.domain.CurrencyEntity;
import me.dhiya.hr.domain.EmployeeEntity;
import me.dhiya.hr.domain.enums.EmployeeStatus;
import me.dhiya.hr.domain.enums.Role;
import me.dhiya.hr.dto.employee.request.CreateEmployeeRequest;
import me.dhiya.hr.dto.employee.request.UpdateEmployeeRequest;
import me.dhiya.hr.dto.employee.response.EmployeeListItemDto;
import me.dhiya.hr.dto.employee.response.EmployeePageDto;
import me.dhiya.hr.dto.employee.response.EmployeeRefDto;
import me.dhiya.hr.repositories.CurrencyRepository;
import me.dhiya.hr.repositories.EmployeeRepository;
import me.dhiya.hr.services.EmployeeService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final CurrencyRepository currencyRepository;
    private final PasswordEncoder passwordEncoder;

    public EmployeeServiceImpl(
            EmployeeRepository employeeRepository,
            CurrencyRepository currencyRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.employeeRepository = employeeRepository;
        this.currencyRepository = currencyRepository;
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
    @Transactional
    public EmployeeEntity createEmployee(CreateEmployeeRequest request, EmployeeEntity createdBy) {
        CurrencyEntity currency = currencyRepository.findByCode(request.getCurrencyCode())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Currency not found: " + request.getCurrencyCode()));

        EmployeeEntity employee = EmployeeEntity.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .department(request.getDepartment())
                .position(request.getPosition())
                .salary(request.getSalary())
                .currency(currency)
                .manager(createdBy)
                .hireDate(request.getHireDate())
                .annualLeaveDays(request.getAnnualLeaveDays() != null ? request.getAnnualLeaveDays() : 30)
                .build();

        return employeeRepository.save(employee);
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeePageDto listEmployees(
            String cursor,
            int size,
            boolean includeInactive,
            String department,
            Role role,
            String search
    ) {
        String searchParam = search != null ? "%" + search.toLowerCase() + "%" : null;
        String roleParam = role != null ? role.name() : null;

        List<EmployeeEntity> results = employeeRepository.findEmployeesPage(
                includeInactive, department, roleParam, searchParam, cursor, size + 1
        );

        boolean hasMore = results.size() > size;
        List<EmployeeEntity> items = hasMore ? results.subList(0, size) : results;
        String nextCursor = hasMore ? items.getLast().getId() : null;

        List<EmployeeListItemDto> dtos = items.stream().map(e -> EmployeeListItemDto.builder()
                .id(e.getId())
                .firstName(e.getFirstName())
                .lastName(e.getLastName())
                .email(e.getEmail())
                .role(e.getRole().name())
                .department(e.getDepartment())
                .position(e.getPosition())
                .status(e.getStatus().name())
                .hireDate(e.getHireDate())
                .manager(e.getManager() != null ? EmployeeRefDto.builder()
                        .id(e.getManager().getId())
                        .firstName(e.getManager().getFirstName())
                        .lastName(e.getManager().getLastName())
                        .department(e.getManager().getDepartment())
                        .build() : null)
                .build()
        ).toList();

        return EmployeePageDto.builder()
                .items(dtos)
                .nextCursor(nextCursor)
                .hasMore(hasMore)
                .size(size)
                .build();
    }

    @Override
    @Transactional
    public EmployeeEntity updateEmployee(String id, UpdateEmployeeRequest request, EmployeeEntity caller) {
        EmployeeEntity target = employeeRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));

        verifyUpdateAccess(caller, target);

        Optional.ofNullable(request.getFirstName()).ifPresent(target::setFirstName);
        Optional.ofNullable(request.getLastName()).ifPresent(target::setLastName);
        Optional.ofNullable(request.getDepartment()).ifPresent(target::setDepartment);
        Optional.ofNullable(request.getPosition()).ifPresent(target::setPosition);
        Optional.ofNullable(request.getSalary()).ifPresent(target::setSalary);
        Optional.ofNullable(request.getAnnualLeaveDays()).ifPresent(target::setAnnualLeaveDays);
        Optional.ofNullable(request.getRole()).ifPresent(target::setRole);
        Optional.ofNullable(request.getCurrencyCode()).ifPresent(code -> target.setCurrency(resolveCurrency(code)));
        Optional.ofNullable(request.getManagerId()).ifPresent(mid -> target.setManager(resolveManager(mid)));

        return employeeRepository.save(target);
    }

    @Override
    @Transactional
    public EmployeeEntity deactivateEmployee(String id, EmployeeEntity caller) {
        EmployeeEntity employee = employeeRepository.findByIdNative(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found"));

        verifyUpdateAccess(caller, employee);

        if (employee.getStatus() == EmployeeStatus.INACTIVE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Employee is already deactivated");
        }

        employee.setStatus(EmployeeStatus.INACTIVE);
        employee.setDeletedAt(Instant.now());
        return employeeRepository.save(employee);
    }

    private void verifyUpdateAccess(EmployeeEntity caller, EmployeeEntity target) {
        if (caller.getRole() == Role.HR && target.getRole() != Role.EMPLOYEE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied.");
        }
    }

    private CurrencyEntity resolveCurrency(String code) {
        return currencyRepository.findByCode(code)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Currency not found: " + code));
    }

    private EmployeeEntity resolveManager(String managerId) {
        return employeeRepository.findById(managerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Manager not found"));
    }
}
