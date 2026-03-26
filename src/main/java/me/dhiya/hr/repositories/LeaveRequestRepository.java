package me.dhiya.hr.repositories;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import me.dhiya.hr.domain.LeaveRequestEntity;
import me.dhiya.hr.domain.enums.LeaveStatus;
import me.dhiya.hr.domain.enums.LeaveType;
import me.dhiya.hr.repositories.projections.LeaveDateRange;
import me.dhiya.hr.repositories.projections.LeaveRequestRow;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface LeaveRequestRepository extends CrudRepository<LeaveRequestEntity, String> {

    java.util.Optional<LeaveRequestEntity> findFirstByEmployee_IdAndStatusInAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            String employeeId,
            List<LeaveStatus> statuses,
            LocalDate endDate,
            LocalDate startDate);

    @Query(value = """
            SELECT
                lr.id              AS id,
                lr.start_date      AS startDate,
                lr.end_date        AS endDate,
                lr.type            AS type,
                lr.status          AS status,
                lr.reason          AS reason,
                lr.review_comment  AS reviewComment,
                lr.created_at      AS createdAt,
                lr.updated_at      AS updatedAt,
                e.id               AS employeeId,
                e.first_name       AS employeeFirstName,
                e.last_name        AS employeeLastName,
                e.department       AS employeeDepartment,
                r.id               AS reviewedById,
                r.first_name       AS reviewedByFirstName,
                r.last_name        AS reviewedByLastName
            FROM leave_requests lr
            JOIN employees e ON e.id = lr.employee_id AND e.status = :employeeStatus
            LEFT JOIN employees r ON r.id = lr.reviewed_by
            WHERE (:employeeId IS NULL OR lr.employee_id = :employeeId)
              AND (:status IS NULL OR lr.status = :status)
              AND (:type IS NULL OR lr.type = :type)
              AND (:fromDate IS NULL OR lr.start_date >= :fromDate)
              AND (:toDate IS NULL OR lr.end_date <= :toDate)
              AND (:cursor IS NULL OR EXISTS (
                   SELECT 1 FROM leave_requests clr
                   WHERE clr.id = :cursor
                   AND (lr.created_at > clr.created_at
                        OR (lr.created_at = clr.created_at AND lr.id > clr.id))
              ))
            ORDER BY lr.created_at ASC, lr.id ASC
            LIMIT :limit
            """, nativeQuery = true)
    List<LeaveRequestRow> findLeaveRequestsPage(
            @Param("employeeId") String employeeId,
            @Param("employeeStatus") String employeeStatus,
            @Param("status") String status,
            @Param("type") String type,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("cursor") String cursor,
            @Param("limit") int limit
    );

    @Query(value = """
            SELECT
                lr.id              AS id,
                lr.start_date      AS startDate,
                lr.end_date        AS endDate,
                lr.type            AS type,
                lr.status          AS status,
                lr.reason          AS reason,
                lr.review_comment  AS reviewComment,
                lr.created_at      AS createdAt,
                lr.updated_at      AS updatedAt,
                e.id               AS employeeId,
                e.first_name       AS employeeFirstName,
                e.last_name        AS employeeLastName,
                e.department       AS employeeDepartment,
                r.id               AS reviewedById,
                r.first_name       AS reviewedByFirstName,
                r.last_name        AS reviewedByLastName
            FROM leave_requests lr
            LEFT JOIN employees e ON e.id = lr.employee_id
            LEFT JOIN employees r ON r.id = lr.reviewed_by
            WHERE lr.id = :id
            """, nativeQuery = true)
    Optional<LeaveRequestRow> findLeaveRequestById(@Param("id") String id);

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
