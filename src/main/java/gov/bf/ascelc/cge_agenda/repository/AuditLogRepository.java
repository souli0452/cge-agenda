package gov.bf.ascelc.cge_agenda.repository;

import gov.bf.ascelc.cge_agenda.dto.ActiveUserDto;
import gov.bf.ascelc.cge_agenda.entities.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    @Query("SELECT a FROM AuditLog a WHERE " +
            "(:action IS NULL OR a.action = CAST(:action AS string)) AND " +
            "(:userEmail IS NULL OR LOWER(a.userEmail) LIKE LOWER(CONCAT('%', CAST(:userEmail AS string), '%'))) AND " +
            "(CAST(CAST(:from AS string) AS timestamp) IS NULL OR a.timestamp >= CAST(CAST(:from AS string) AS timestamp)) AND " +
            "(CAST(CAST(:to AS string) AS timestamp) IS NULL OR a.timestamp <= CAST(CAST(:to AS string) AS timestamp)) " +
            "ORDER BY a.timestamp DESC")
    Page<AuditLog> findFiltered(
            @Param("action") String action,
            @Param("userEmail") String userEmail,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable
    );

    @Query("SELECT new gov.bf.ascelc.cge_agenda.dto.ActiveUserDto(" +
            "a.userEmail, a.userFullName, a.userRole, MAX(a.timestamp)) " +
            "FROM AuditLog a WHERE a.action = 'CONNEXION' AND a.timestamp >= :since " +
            "GROUP BY a.userEmail, a.userFullName, a.userRole " +
            "ORDER BY MAX(a.timestamp) DESC")
    List<ActiveUserDto> findRecentlyActiveUsers(@Param("since") LocalDateTime since);
}
