package gov.bf.ascelc.cge_agenda.dto;

import gov.bf.ascelc.cge_agenda.enums.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDto {
    private UUID id;
    private NotificationType type;
    private UUID eventId;
    private String message;
    private boolean lue;
    private LocalDateTime createdAt;
}
