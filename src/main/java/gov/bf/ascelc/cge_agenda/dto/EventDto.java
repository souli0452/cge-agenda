package gov.bf.ascelc.cge_agenda.dto;

import gov.bf.ascelc.cge_agenda.abstracts.AuditEntityDto;
import gov.bf.ascelc.cge_agenda.enums.EventStatus;
import gov.bf.ascelc.cge_agenda.enums.EventType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DTO pour créer un événement avec participants et horaires
 */
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
@AllArgsConstructor

public class EventDto extends AuditEntityDto {

    @NotBlank(message = "Le titre est obligatoire")
    @Size(max = 255)
    private String title;

    private String description;

    @NotNull(message = "La date de début est obligatoire")
    private LocalDate startDate;

    @NotNull(message = "La date de fin est obligatoire")
    private LocalDate endDate;

    /**
     * MODE A : Horaires globaux (optionnel si schedules fournis)
     */
    private LocalTime globalStartTime;
    private LocalTime globalEndTime;

    @Size(max = 500)
    private String meetingLink;

    @Size(max = 100)
    private String pays;

    @NotNull(message = "Le statut est obligatoire")
    private EventStatus status;

    @NotNull(message = "Le type est obligatoire")
    private EventType type;

    /**
     * MODE B : Horaires spécifiques par jour
     */
    @Valid
    @Builder.Default
    private List<ScheduleDto> schedules = new ArrayList<>();

    /**
     * Liste des participants à créer ou à associer
     */
    @Valid
    @Builder.Default
    private List<ParticipantDto> participants = new ArrayList<>();

    public boolean isGlobalScheduleMode() {
        return (schedules == null || schedules.isEmpty()) &&
                globalStartTime != null && globalEndTime != null;
    }

    public boolean isCustomScheduleMode() {
        return schedules != null && !schedules.isEmpty();
    }
}
