package me.dhiya.hr.repositories.projections;

import java.time.Instant;
import java.time.LocalDate;

public interface LeaveRequestRow {
    String getId();
    LocalDate getStartDate();
    LocalDate getEndDate();
    String getType();
    String getStatus();
    String getReason();
    String getReviewComment();
    Instant getCreatedAt();
    Instant getUpdatedAt();
    String getEmployeeId();
    String getEmployeeFirstName();
    String getEmployeeLastName();
    String getEmployeeDepartment();
    String getReviewedById();
    String getReviewedByFirstName();
    String getReviewedByLastName();
}
