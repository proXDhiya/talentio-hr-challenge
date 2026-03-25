package me.dhiya.hr.services;

import me.dhiya.hr.TestDataUtil;
import me.dhiya.hr.domain.EmployeeEntity;
import me.dhiya.hr.domain.LeaveRequestEntity;
import me.dhiya.hr.domain.enums.EmployeeStatus;
import me.dhiya.hr.domain.enums.LeaveStatus;
import me.dhiya.hr.domain.enums.LeaveType;
import me.dhiya.hr.domain.enums.Role;
import me.dhiya.hr.dto.employee.request.CreateEmployeeRequest;
import me.dhiya.hr.repositories.CurrencyRepository;
import me.dhiya.hr.repositories.EmployeeRepository;
import me.dhiya.hr.repositories.LeaveRequestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class EmployeeServiceTests {

    @Autowired private EmployeeService employeeService;
    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private LeaveRequestRepository leaveRequestRepository;
    @Autowired private CurrencyRepository currencyRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    @Test
    void authenticateReturnsEmployeeWithValidCredentials() {
        EmployeeEntity employee = TestDataUtil.createEmployee();
        String rawPassword = employee.getPassword();
        employee.setPassword(passwordEncoder.encode(rawPassword));
        employeeRepository.save(employee);

        Optional<EmployeeEntity> result = employeeService.authenticate(employee.getEmail(), rawPassword);

        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo(employee.getEmail());
    }

    @Test
    void authenticateReturnsEmptyWithWrongPassword() {
        EmployeeEntity employee = TestDataUtil.createEmployee();
        employee.setPassword(passwordEncoder.encode(employee.getPassword()));
        employeeRepository.save(employee);

        Optional<EmployeeEntity> result = employeeService.authenticate(employee.getEmail(), "wrong-password");

        assertThat(result).isEmpty();
    }

    @Test
    void authenticateReturnsEmptyWithNonExistentEmail() {
        Optional<EmployeeEntity> result = employeeService.authenticate("nobody@company.com", "any-password");

        assertThat(result).isEmpty();
    }

    @Test
    void authenticateReturnsEmptyForInactiveEmployee() {
        EmployeeEntity employee = TestDataUtil.createInactiveEmployee();
        String rawPassword = employee.getPassword();
        employee.setPassword(passwordEncoder.encode(rawPassword));
        employeeRepository.save(employee);

        Optional<EmployeeEntity> result = employeeService.authenticate(employee.getEmail(), rawPassword);

        assertThat(result).isEmpty();
    }

    @Test
    void setupCreatesFirstEmployeeWithManagerRole() {
        EmployeeEntity saved = employeeService.setup(TestDataUtil.createEmployee());

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getRole()).isEqualTo(Role.MANAGER);
    }

    @Test
    void setupSetsHireDateToToday() {
        EmployeeEntity saved = employeeService.setup(TestDataUtil.createEmployee());

        assertThat(saved.getHireDate()).isEqualTo(LocalDate.now());
    }

    @Test
    void setupEncodesPassword() {
        EmployeeEntity employee = TestDataUtil.createEmployee();
        String rawPassword = employee.getPassword();

        EmployeeEntity saved = employeeService.setup(employee);

        assertThat(saved.getPassword()).isNotEqualTo(rawPassword);
        assertThat(passwordEncoder.matches(rawPassword, saved.getPassword())).isTrue();
    }

    @Test
    void setupThrowsConflictWhenEmployeesAlreadyExist() {
        EmployeeEntity existing = TestDataUtil.createEmployee();
        existing.setPassword(passwordEncoder.encode(existing.getPassword()));
        existing.setRole(Role.MANAGER);
        existing.setHireDate(LocalDate.now());
        employeeRepository.save(existing);

        assertThatThrownBy(() -> employeeService.setup(TestDataUtil.createEmployee()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Setup has already been completed");
    }

    @Test
    void getProfileReturnsEmployeeById() {
        EmployeeEntity employee = TestDataUtil.createEmployee();
        employee.setPassword(passwordEncoder.encode(employee.getPassword()));
        employee.setRole(Role.EMPLOYEE);
        employee.setHireDate(LocalDate.now());
        EmployeeEntity saved = employeeRepository.save(employee);

        EmployeeEntity result = employeeService.getProfile(saved.getId());

        assertThat(result.getId()).isEqualTo(saved.getId());
        assertThat(result.getEmail()).isEqualTo(saved.getEmail());
    }

    @Test
    void getProfileThrows404WhenEmployeeNotFound() {
        assertThatThrownBy(() -> employeeService.getProfile("non-existent-id"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Employee not found");
    }

    @Test
    void calculateUsedLeaveDaysReturnsZeroWhenNoLeaves() {
        EmployeeEntity employee = savedActiveEmployee(LocalDate.of(2020, 1, 15));

        assertThat(employeeService.calculateUsedLeaveDays(employee)).isZero();
    }

    @Test
    void calculateUsedLeaveDaysCountsApprovedLeavesInCurrentCycle() {
        EmployeeEntity employee = savedActiveEmployee(LocalDate.of(2020, 3, 1));
        LocalDate cycleStart = LocalDate.now().withDayOfMonth(1).withMonth(3);
        if (LocalDate.now().isBefore(cycleStart)) cycleStart = cycleStart.minusYears(1);

        saveLeave(employee, cycleStart.plusDays(5), cycleStart.plusDays(9), LeaveStatus.APPROVED);

        assertThat(employeeService.calculateUsedLeaveDays(employee)).isEqualTo(5);
    }

    @Test
    void calculateUsedLeaveDaysIgnoresPendingLeaves() {
        EmployeeEntity employee = savedActiveEmployee(LocalDate.of(2020, 3, 1));
        LocalDate cycleStart = LocalDate.now().withDayOfMonth(1).withMonth(3);
        if (LocalDate.now().isBefore(cycleStart)) cycleStart = cycleStart.minusYears(1);

        saveLeave(employee, cycleStart.plusDays(5), cycleStart.plusDays(9), LeaveStatus.PENDING);

        assertThat(employeeService.calculateUsedLeaveDays(employee)).isZero();
    }

    @Test
    void calculateUsedLeaveDaysIgnoresRejectedLeaves() {
        EmployeeEntity employee = savedActiveEmployee(LocalDate.of(2020, 3, 1));
        LocalDate cycleStart = LocalDate.now().withDayOfMonth(1).withMonth(3);
        if (LocalDate.now().isBefore(cycleStart)) cycleStart = cycleStart.minusYears(1);

        saveLeave(employee, cycleStart.plusDays(5), cycleStart.plusDays(9), LeaveStatus.REJECTED);

        assertThat(employeeService.calculateUsedLeaveDays(employee)).isZero();
    }

    @Test
    void calculateUsedLeaveDaysIgnoresLeavesOutsideCurrentCycle() {
        EmployeeEntity employee = savedActiveEmployee(LocalDate.of(2020, 3, 1));
        saveLeave(employee, LocalDate.now().minusYears(5), LocalDate.now().minusYears(5).plusDays(4), LeaveStatus.APPROVED);

        assertThat(employeeService.calculateUsedLeaveDays(employee)).isZero();
    }

    @Test
    void calculateUsedLeaveDaysSumsMultipleApprovedLeaves() {
        EmployeeEntity employee = savedActiveEmployee(LocalDate.of(2020, 3, 1));
        LocalDate cycleStart = LocalDate.now().withDayOfMonth(1).withMonth(3);
        if (LocalDate.now().isBefore(cycleStart)) cycleStart = cycleStart.minusYears(1);

        saveLeave(employee, cycleStart.plusDays(5), cycleStart.plusDays(6), LeaveStatus.APPROVED);
        saveLeave(employee, cycleStart.plusDays(10), cycleStart.plusDays(12), LeaveStatus.APPROVED);

        assertThat(employeeService.calculateUsedLeaveDays(employee)).isEqualTo(5);
    }

    @Test
    void createEmployeeSavesEmployeeWithCorrectFields() {
        EmployeeEntity manager = savedManager();
        EmployeeEntity result = employeeService.createEmployee(buildCreateRequest("USD", null), manager);

        assertThat(result.getId()).isNotNull();
        assertThat(result.getEmail()).isEqualTo("john@company.com");
        assertThat(result.getFirstName()).isEqualTo("John");
        assertThat(result.getRole()).isEqualTo(Role.EMPLOYEE);
        assertThat(result.getSalary()).isEqualByComparingTo(new BigDecimal("3000.00"));
        assertThat(result.getStatus()).isEqualTo(EmployeeStatus.ACTIVE);
    }

    @Test
    void createEmployeeSetsManagerToCreatedBy() {
        EmployeeEntity manager = savedManager();
        EmployeeEntity result = employeeService.createEmployee(buildCreateRequest("USD", null), manager);

        assertThat(result.getManager()).isNotNull();
        assertThat(result.getManager().getId()).isEqualTo(manager.getId());
    }

    @Test
    void createEmployeeSetsCurrency() {
        EmployeeEntity manager = savedManager();
        EmployeeEntity result = employeeService.createEmployee(buildCreateRequest("EUR", null), manager);

        assertThat(result.getCurrency()).isNotNull();
        assertThat(result.getCurrency().getCode()).isEqualTo("EUR");
    }

    @Test
    void createEmployeeEncodesPassword() {
        EmployeeEntity manager = savedManager();
        EmployeeEntity result = employeeService.createEmployee(buildCreateRequest("USD", null), manager);

        assertThat(passwordEncoder.matches("TempPass123!", result.getPassword())).isTrue();
    }

    @Test
    void createEmployeeDefaultsAnnualLeaveDaysTo30WhenNotProvided() {
        EmployeeEntity result = employeeService.createEmployee(buildCreateRequest("USD", null), savedManager());

        assertThat(result.getAnnualLeaveDays()).isEqualTo(30);
    }

    @Test
    void createEmployeeUsesProvidedAnnualLeaveDays() {
        EmployeeEntity result = employeeService.createEmployee(buildCreateRequest("USD", 25), savedManager());

        assertThat(result.getAnnualLeaveDays()).isEqualTo(25);
    }

    @Test
    void createEmployeeThrows400WhenCurrencyCodeNotFound() {
        assertThatThrownBy(() -> employeeService.createEmployee(buildCreateRequest("XYZ", null), savedManager()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Currency not found: XYZ");
    }

    private EmployeeEntity savedActiveEmployee(LocalDate hireDate) {
        EmployeeEntity employee = TestDataUtil.createEmployee();
        employee.setPassword(passwordEncoder.encode(employee.getPassword()));
        employee.setRole(Role.EMPLOYEE);
        employee.setHireDate(hireDate);
        return employeeRepository.save(employee);
    }

    private EmployeeEntity savedManager() {
        EmployeeEntity manager = TestDataUtil.createEmployee();
        manager.setPassword(passwordEncoder.encode(manager.getPassword()));
        manager.setRole(Role.MANAGER);
        manager.setHireDate(LocalDate.now());
        return employeeRepository.save(manager);
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

    private CreateEmployeeRequest buildCreateRequest(String currencyCode, Integer annualLeaveDays) {
        return CreateEmployeeRequest.builder()
                .firstName("John").lastName("Doe").email("john@company.com")
                .password("TempPass123!").role(Role.EMPLOYEE)
                .department("Engineering").position("Software Engineer")
                .salary(new BigDecimal("3000.00")).currencyCode(currencyCode)
                .hireDate(LocalDate.now()).annualLeaveDays(annualLeaveDays)
                .build();
    }
}
