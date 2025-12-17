package gov.bf.ascelc.cge_agenda.dto;

import gov.bf.ascelc.cge_agenda.abstracts.AuditEntityDto;
import gov.bf.ascelc.cge_agenda.enums.EventStatus;
import gov.bf.ascelc.cge_agenda.enums.EventType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Getter
@Setter
public class EventDto extends AuditEntityDto {

    private String title;

    private String description;

    private LocalDate startDate;

    private LocalDate endDate;

    private String meetingLink;

    private String pays;

    private UUID eventId;

    @NotNull(message = "Le statut est obligatoire")
    private EventStatus statut;

    @NotNull(message = "Le Type est obligatoire")
    private EventType type;

    // Liste des horaires
    private List<ScheduleDto> horaires = new ArrayList<>();

    // Liste des participants (IDs seulement pour la création/modification)
    private List<UUID> participantIds = new ArrayList<>();

    // Liste des participants (avec détails pour les réponses)
    private List<ParticipantDto> participants = new ArrayList<>();

    // Liste des fichiers
    private List<FileDto> files = new ArrayList<>();

    /**
     * Méthode utilitaire pour vérifier si l'événement est multi-jours
     */
    public boolean isMultiJours() {
        return startDate != null && endDate != null && !startDate.equals(endDate);
    }

    /**
     * Méthode utilitaire pour obtenir le nombre de jours
     */
    public long getNombreJours() {
        if (startDate == null || endDate == null) return 0;
        return java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
    }

}
