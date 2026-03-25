package me.dhiya.hr;

import me.dhiya.hr.domain.EmployeeEntity;
import me.dhiya.hr.domain.enums.EmployeeStatus;
import me.dhiya.hr.domain.enums.Role;
import me.dhiya.hr.dto.auth.request.SetupRequest;

import java.time.LocalDate;
import java.util.UUID;

public final class TestDataUtil {
    private TestDataUtil() {}

    private static String randomString() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }

    private static String randomEmail() {
        return randomString() + "@company.com";
    }

    private static String randomPassword() {
        return "Pwd-" + UUID.randomUUID();
    }

    public static EmployeeEntity createEmployee() {
        return EmployeeEntity.builder()
                .firstName(randomString())
                .lastName(randomString())
                .email(randomEmail())
                .password(randomPassword())
                .role(Role.EMPLOYEE)
                .hireDate(LocalDate.of(2024, 1, 15))
                .build();
    }

    public static EmployeeEntity createInactiveEmployee() {
        return EmployeeEntity.builder()
                .firstName(randomString())
                .lastName(randomString())
                .email(randomEmail())
                .password(randomPassword())
                .role(Role.EMPLOYEE)
                .hireDate(LocalDate.of(2024, 1, 15))
                .status(EmployeeStatus.INACTIVE)
                .build();
    }

    public static SetupRequest createSetupRequest() {
        return SetupRequest.builder()
                .firstName(randomString())
                .lastName(randomString())
                .email(randomEmail())
                .password(randomPassword())
                .department(randomString())
                .build();
    }
}
