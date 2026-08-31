package gov.bf.ascelc.cge_agenda.controller;

import gov.bf.ascelc.cge_agenda.dto.NotificationDto;
import gov.bf.ascelc.cge_agenda.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static gov.bf.ascelc.cge_agenda.utils.ApiUrls.*;

@RestController
@RequestMapping(NOTIFICATION_ROOT_URL)
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<NotificationDto>> getMesNotifications() {
        return ResponseEntity.ok(notificationService.getMesNotifications());
    }

    @GetMapping(NOTIFICATION_COUNT_NON_LUES)
    public ResponseEntity<Long> countNonLues() {
        return ResponseEntity.ok(notificationService.countNonLues());
    }

    @PatchMapping(NOTIFICATION_MARK_LUE)
    public ResponseEntity<Void> marquerLue(@PathVariable UUID id) {
        notificationService.marquerLue(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping(NOTIFICATION_MARK_ALL_LUES)
    public ResponseEntity<Void> marquerToutesLues() {
        notificationService.marquerToutesLues();
        return ResponseEntity.noContent().build();
    }
}
