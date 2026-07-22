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

    private LocalTime globalStartTime;
    private LocalTime globalEndTime;

    @Size(max = 500)
    private String meetingLink;

    @Size(max = 100)
    private String pays;

    @Size(max = 100)
    private String ville;

    @NotNull(message = "Le statut est obligatoire")
    private EventStatus status;

    @NotNull(message = "Le type est obligatoire")
    private EventType type;

    @Valid
    @Builder.Default
    private List<ScheduleDto> schedules = new ArrayList<>();

    private List<FileDto> files;

    @Valid
    @Builder.Default
    private List<ParticipantDto> participants = new ArrayList<>();

    private List<String> structures;

    // ==========================================
    // WORKFLOW DE VALIDATION CGE
    // ==========================================
    private String changeSuggestions;
    private String rejectionReason;
    private String validationComment;
    private String creatorEmail;
    private String creatorUsername;
    private String creatorRole;
    private String delegueNom;
    private String delegueEmail;
    private String delegueMotif;
    private boolean estDelegue;
    private java.time.LocalDateTime delegueDate;
    private String delegueParEmail;

    private String compteRenduPoints;
    private String compteRenduDecisions;
    private String compteRenduActions;
    private String compteRenduRedigePar;
    private java.time.LocalDateTime compteRenduDate;


    public boolean isGlobalScheduleMode() {
        return globalStartTime != null
                && globalEndTime != null
                && (schedules == null || schedules.isEmpty());
    }


    public boolean isCustomScheduleMode() {
        return schedules != null
                && !schedules.isEmpty()
                && globalStartTime == null
                && globalEndTime == null;
    }
}