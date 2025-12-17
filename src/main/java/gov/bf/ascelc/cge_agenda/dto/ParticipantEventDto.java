package gov.bf.ascelc.cge_agenda.dto;

import gov.bf.ascelc.cge_agenda.abstracts.AuditEntityDto;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public class ParticipantEventDto extends AuditEntityDto {

    @NotNull(message = "L'ID de l'événement est obligatoire")
    private UUID eventId;

    @NotNull(message = "L'ID du participant est obligatoire")
    private UUID participantId;


    private ParticipantDto participant;
}
