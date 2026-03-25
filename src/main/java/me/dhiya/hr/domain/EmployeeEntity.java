package me.dhiya.hr.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import me.dhiya.hr.domain.enums.EmployeeStatus;
import me.dhiya.hr.domain.enums.Role;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@SQLRestriction("status = 'ACTIVE'")
@Entity
@Table(name = "employees", uniqueConstraints = @UniqueConstraint(columnNames = "email"))
public class EmployeeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @Column(name = "email", unique = true, nullable = false)
    private String email;

    @Column(name = "password", nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role;

    @Column(name = "department")
    private String department;

    @Column(name = "position")
    private String position;

    @Builder.Default
    @Column(name = "salary", nullable = false, precision = 10, scale = 2, columnDefinition = "DECIMAL(10,2) DEFAULT 0.00")
    private BigDecimal salary = BigDecimal.ZERO;

    @ManyToOne
    @JoinColumn(name = "currency_id")
    private CurrencyEntity currency;

    @Column(name = "hire_date", nullable = false)
    private LocalDate hireDate;

    @Builder.Default
    @Column(name = "annual_leave_days")
    private Integer annualLeaveDays = 30;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private EmployeeStatus status = EmployeeStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;
}
