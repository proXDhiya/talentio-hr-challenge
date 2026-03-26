package me.dhiya.hr.repositories.projections;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public interface LeaveRequestRow {
    String getId();
    LocalDate getStartDate();
    LocalDate getEndDate();
    String getType();
    String getStatus();
    String getReason();
    String getReviewComment();
    OffsetDateTime getCreatedAt();
    String getEmployeeId();
    String getEmployeeFirstName();
    String getEmployeeLastName();
    String getEmployeeDepartment();
    String getReviewedById();
    String getReviewedByFirstName();
    String getReviewedByLastName();
}
