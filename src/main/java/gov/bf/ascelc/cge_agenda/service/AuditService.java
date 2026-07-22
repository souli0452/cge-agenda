package gov.bf.ascelc.cge_agenda.service;

import gov.bf.ascelc.cge_agenda.dto.ActiveUserDto;
import gov.bf.ascelc.cge_agenda.dto.AuditLogDto;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditService {

    void logAction(
            String action,
            String entityType,
            String entityId,
            String entityTitle,
            String details,
            HttpServletRequest request
    );

    Page<AuditLogDto> getPaged(
            String action,
            String userEmail,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable
    );

    List<ActiveUserDto> getRecentlyActiveUsers();
}
