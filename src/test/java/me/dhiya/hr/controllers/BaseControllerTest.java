package me.dhiya.hr.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import me.dhiya.hr.domain.EmployeeEntity;
import me.dhiya.hr.domain.enums.EmployeeStatus;
import me.dhiya.hr.domain.enums.Role;
import me.dhiya.hr.repositories.EmployeeRepository;
import me.dhiya.hr.repositories.LeaveRequestRepository;
import me.dhiya.hr.services.JwtService;
import java.time.LocalDate;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public abstract class BaseControllerTest {

    @Autowired protected WebApplicationContext wac;
    @Autowired protected EmployeeRepository employeeRepository;
    @Autowired protected LeaveRequestRepository leaveRequestRepository;
    @Autowired protected PasswordEncoder passwordEncoder;
    @Autowired protected JwtService jwtService;

    protected MockMvc mockMvc;

    @BeforeEach
    void setupMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    protected EmployeeEntity saveEmployee(String firstName, String lastName, String email, Role role) {
        return saveEmployee(firstName, lastName, email, "Engineering", role, EmployeeStatus.ACTIVE);
    }

    protected EmployeeEntity saveEmployee(String firstName, String lastName, String email, Role role, EmployeeStatus status) {
        return saveEmployee(firstName, lastName, email, "Engineering", role, status);
    }

    protected EmployeeEntity saveEmployee(String firstName, String lastName, String email, String department, Role role, EmployeeStatus status) {
        return employeeRepository.save(EmployeeEntity.builder()
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .password(passwordEncoder.encode("Pass123!"))
                .role(role)
                .department(department)
                .hireDate(LocalDate.now())
                .status(status != null ? status : EmployeeStatus.ACTIVE)
                .build());
    }

    protected EmployeeEntity saveEmployeeWithManager(String firstName, String lastName, String email, EmployeeEntity manager) {
        return employeeRepository.save(EmployeeEntity.builder()
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .password(passwordEncoder.encode("Pass123!"))
                .role(Role.EMPLOYEE)
                .department("Engineering")
                .hireDate(LocalDate.now())
                .status(EmployeeStatus.ACTIVE)
                .manager(manager)
                .build());
    }

    protected String token(EmployeeEntity employee) {
        return jwtService.generateToken(employee);
    }
}
