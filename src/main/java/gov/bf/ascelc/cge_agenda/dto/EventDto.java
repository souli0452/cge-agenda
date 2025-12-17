package gov.bf.ascelc.cge_agenda.dto;

import gov.bf.ascelc.cge_agenda.abstracts.AuditEntityDto;
import gov.bf.ascelc.cge_agenda.enums.EventStatus;
import gov.bf.ascelc.cge_agenda.enums.EventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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

    @NotBlank(message = "Le titre est obligatoire")
    @Size(max = 255, message = "Le titre ne peut pas dépasser 255 caractères")
    private String title;

    private String description;

    @NotNull(message = "La date de début est obligatoire")
    private LocalDate startDate;

    @NotNull(message = "La date de fin est obligatoire")
    private LocalDate endDate;

    @Size(max = 500, message = "Le lien ne peut pas dépasser 500 caractères")
    private String meetingLink;

    @Size(max = 100, message = "Le nom du pays ne peut pas dépasser 100 caractères")
    private String pays;


    @NotNull(message = "Le statut est obligatoire")
    private EventStatus status;

    @NotNull(message = "Le Type est obligatoire")
    private EventType type;

    // Liste des horaires
    private List<ScheduleDto> schedule = new ArrayList<>();

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
