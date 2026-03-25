package me.dhiya.hr.repositories;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import me.dhiya.hr.domain.LeaveRequestEntity;
import me.dhiya.hr.domain.enums.LeaveStatus;
import me.dhiya.hr.repositories.projections.LeaveDateRange;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface LeaveRequestRepository extends CrudRepository<LeaveRequestEntity, String> {

    java.util.Optional<LeaveRequestEntity> findFirstByEmployee_IdAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            String employeeId,
            List<LeaveStatus> statuses,
            LocalDate endDate,
            LocalDate startDate);

    @Query("SELECT l.startDate as startDate, l.endDate as endDate " +
           "FROM LeaveRequestEntity l " +
           "WHERE l.employee.id = :employeeId " +
           "AND l.status = :status " +
           "AND l.startDate <= :to AND l.endDate >= :from")
    List<LeaveDateRange> findApprovedLeaveDates(
            @Param("employeeId") String employeeId,
            @Param("status") LeaveStatus status,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);
}
