package gov.bf.ascelc.cge_agenda.entities;

import gov.bf.ascelc.cge_agenda.enums.NotificationType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "notification")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "destinataire_email", nullable = false, length = 255)
    private String destinataireEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private NotificationType type;

    @Column(name = "event_id")
    private UUID eventId;

    @Column(name = "message", length = 500)
    private String message;

    @Column(name = "lue", nullable = false)
    @Builder.Default
    private boolean lue = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
