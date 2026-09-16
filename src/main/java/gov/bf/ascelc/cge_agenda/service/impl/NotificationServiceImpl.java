package gov.bf.ascelc.cge_agenda.service.impl;

import gov.bf.ascelc.cge_agenda.dto.NotificationDto;
import gov.bf.ascelc.cge_agenda.entities.Notification;
import gov.bf.ascelc.cge_agenda.enums.NotificationType;
import gov.bf.ascelc.cge_agenda.repository.NotificationRepository;
import gov.bf.ascelc.cge_agenda.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;

    @Override
    @Transactional
    public void notifier(String destinataireEmail, NotificationType type, UUID eventId, String message) {
        if (destinataireEmail == null || destinataireEmail.isBlank()) {
            return;
        }
        try {
            notificationRepository.save(Notification.builder()
                    .destinataireEmail(destinataireEmail)
                    .type(type)
                    .eventId(eventId)
                    .message(message)
                    .lue(false)
                    .createdAt(LocalDateTime.now())
                    .build());
        } catch (Exception e) {
            log.warn("Impossible de créer la notification '{}' pour {}", type, destinataireEmail, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificationDto> getMesNotifications() {
        String email = currentUserEmail();
        if (email == null) {
            return List.of();
        }
        return notificationRepository.findByDestinataireEmailIgnoreCaseOrderByCreatedAtDesc(email)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countNonLues() {
        String email = currentUserEmail();
        return email == null ? 0 : notificationRepository.countByDestinataireEmailIgnoreCaseAndLueFalse(email);
    }

    @Override
    @Transactional
    public void marquerLue(UUID id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification non trouvée : " + id));

        String email = currentUserEmail();
        if (email == null || !email.equalsIgnoreCase(notification.getDestinataireEmail())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cette notification ne vous appartient pas");
        }

        notification.setLue(true);
        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public void marquerToutesLues() {
        String email = currentUserEmail();
        if (email != null) {
            notificationRepository.marquerToutesLues(email);
        }
    }

    private NotificationDto toDto(Notification n) {
        return NotificationDto.builder()
                .id(n.getId())
                .type(n.getType())
                .eventId(n.getEventId())
                .message(n.getMessage())
                .lue(n.isLue())
                .createdAt(n.getCreatedAt())
                .build();
    }

    private String currentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            String email = jwt.getClaim("email");
            return email != null ? email : jwt.getClaim("preferred_username");
        }
        return null;
    }
}
