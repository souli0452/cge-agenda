package gov.bf.ascelc.cge_agenda.controller;

import gov.bf.ascelc.cge_agenda.dto.ActiveUserDto;
import gov.bf.ascelc.cge_agenda.dto.AuditLogDto;
import gov.bf.ascelc.cge_agenda.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PagedModel;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

import static gov.bf.ascelc.cge_agenda.utils.ApiUrls.AUDIT_PAGED;
import static gov.bf.ascelc.cge_agenda.utils.ApiUrls.AUDIT_RECENTLY_ACTIVE;
import static gov.bf.ascelc.cge_agenda.utils.ApiUrls.AUDIT_ROOT_URL;

@RestController
@RequestMapping(AUDIT_ROOT_URL)
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;

    @GetMapping(AUDIT_PAGED)
    public ResponseEntity<PagedModel<AuditLogDto>> getPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String userEmail,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        Page<AuditLogDto> result = auditService.getPaged(
                action, userEmail, from, to, PageRequest.of(page, size));
        return ResponseEntity.ok(new PagedModel<>(result));
    }

    @GetMapping(AUDIT_RECENTLY_ACTIVE)
    public ResponseEntity<List<ActiveUserDto>> getRecentlyActiveUsers() {
        return ResponseEntity.ok(auditService.getRecentlyActiveUsers());
    }
}
