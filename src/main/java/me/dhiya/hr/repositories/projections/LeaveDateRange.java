package me.dhiya.hr.repositories.projections;

import java.time.LocalDate;

public interface LeaveDateRange {
    LocalDate getStartDate();
    LocalDate getEndDate();
}
