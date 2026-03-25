package me.dhiya.hr.services;

import me.dhiya.hr.TestDataUtil;
import me.dhiya.hr.domain.EmployeeEntity;
import me.dhiya.hr.repositories.EmployeeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class EmployeeServiceImplIntegrationTests {

    private final EmployeeService employeeService;
    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public EmployeeServiceImplIntegrationTests(
            EmployeeService employeeService,
            EmployeeRepository employeeRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.employeeService = employeeService;
        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Test
    public void testAuthenticateReturnsEmployeeWithValidCredentials() {
        EmployeeEntity employee = TestDataUtil.createEmployee();
        String rawPassword = employee.getPassword();
        employee.setPassword(passwordEncoder.encode(rawPassword));
        employeeRepository.save(employee);

        Optional<EmployeeEntity> result = employeeService.authenticate(employee.getEmail(), rawPassword);

        assertThat(result).isPresent();
        assertThat(result.get().getEmail()).isEqualTo(employee.getEmail());
    }

    @Test
    public void testAuthenticateReturnsEmptyWithWrongPassword() {
        EmployeeEntity employee = TestDataUtil.createEmployee();
        employee.setPassword(passwordEncoder.encode(employee.getPassword()));
        employeeRepository.save(employee);

        Optional<EmployeeEntity> result = employeeService.authenticate(employee.getEmail(), "wrong-password");

        assertThat(result).isEmpty();
    }

    @Test
    public void testAuthenticateReturnsEmptyWithNonExistentEmail() {
        Optional<EmployeeEntity> result = employeeService.authenticate("nobody@company.com", "any-password");

        assertThat(result).isEmpty();
    }

    @Test
    public void testAuthenticateReturnsEmptyForInactiveEmployee() {
        EmployeeEntity employee = TestDataUtil.createInactiveEmployee();
        String rawPassword = employee.getPassword();
        employee.setPassword(passwordEncoder.encode(rawPassword));
        employeeRepository.save(employee);

        Optional<EmployeeEntity> result = employeeService.authenticate(employee.getEmail(), rawPassword);

        assertThat(result).isEmpty();
    }
}
