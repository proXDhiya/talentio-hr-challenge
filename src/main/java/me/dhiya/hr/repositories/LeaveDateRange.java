package me.dhiya.hr.repositories;

import java.time.LocalDate;

public interface LeaveDateRange {
    LocalDate getStartDate();
    LocalDate getEndDate();
}
