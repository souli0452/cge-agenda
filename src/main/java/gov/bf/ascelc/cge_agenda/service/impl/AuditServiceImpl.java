package gov.bf.ascelc.cge_agenda.service.impl;

import gov.bf.ascelc.cge_agenda.dto.ActiveUserDto;
import gov.bf.ascelc.cge_agenda.dto.AuditLogDto;
import gov.bf.ascelc.cge_agenda.entities.AuditLog;
import gov.bf.ascelc.cge_agenda.mapper.AuditLogMapper;
import gov.bf.ascelc.cge_agenda.repository.AuditLogRepository;
import gov.bf.ascelc.cge_agenda.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    /** Rôles métier par ordre de priorité pour choisir le rôle "principal" affiché dans le journal. */
    private static final List<String> ROLE_PRIORITY = List.of(
            "ADMIN", "CGE", "DIRECTEUR_CABINET", "PROTOCOLE", "SECRETAIRE", "DELEGUE"
    );

    private final AuditLogRepository auditLogRepository;
    private final AuditLogMapper auditLogMapper;

    @Override
    @Transactional
    public void logAction(
            String action,
            String entityType,
            String entityId,
            String entityTitle,
            String details,
            HttpServletRequest request
    ) {
        try {
            AuditLog.AuditLogBuilder builder = AuditLog.builder()
                    .timestamp(LocalDateTime.now())
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .entityTitle(entityTitle)
                    .details(details);

            populateCurrentUser(builder);

            if (request != null) {
                builder.ipAddress(extractClientIp(request));
                builder.userAgent(request.getHeader("User-Agent"));
            }

            auditLogRepository.save(builder.build());
        } catch (Exception e) {
            // Le suivi d'audit ne doit jamais faire échouer l'action métier qu'il journalise.
            log.warn("Impossible d'enregistrer l'entrée d'audit pour l'action '{}'", action, e);
        }
    }

    @Override
    public Page<AuditLogDto> getPaged(
            String action,
            String userEmail,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable
    ) {
        return auditLogRepository
                .findFiltered(action, userEmail, from, to, pageable)
                .map(auditLogMapper::toDto);
    }

    @Override
    public List<ActiveUserDto> getRecentlyActiveUsers() {
        return auditLogRepository.findRecentlyActiveUsers(LocalDateTime.now().minusHours(24));
    }

    private void populateCurrentUser(AuditLog.AuditLogBuilder builder) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            return;
        }

        String firstName = jwt.getClaim("given_name");
        String lastName = jwt.getClaim("family_name");
        String fullName = ((firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "")).trim();

        builder.userId(jwt.getSubject())
                .userEmail(jwt.getClaim("email"))
                .userFullName(fullName.isBlank() ? jwt.getClaim("preferred_username") : fullName)
                .userRole(extractPrimaryRole(authentication));
    }

    private String extractPrimaryRole(Authentication authentication) {
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(a -> a.startsWith("ROLE_") ? a.substring(5) : a)
                .toList();

        return ROLE_PRIORITY.stream()
                .filter(roles::contains)
                .findFirst()
                .orElse(roles.isEmpty() ? null : roles.get(0));
    }

    private String extractClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
