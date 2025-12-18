package gov.bf.ascelc.cge_agenda.dto;

import gov.bf.ascelc.cge_agenda.abstracts.AuditEntityDto;
import gov.bf.ascelc.cge_agenda.enums.ParticipantType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Getter
@Setter
public class ParticipantDto extends AuditEntityDto {

    @NotBlank(message = "Le nom est obligatoire")
    private String currentLastName;

    @NotBlank(message = "Le prénom est obligatoire")
    private String currentFirstName;;

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Format d'email invalide")
    private String email;

    private String  phoneNumber;

    private String  jobTitle;

    private String  organization;

    @NotNull(message = "Le type de participant est obligatoire")
    private ParticipantType participantType;

    @NotNull(message = "L'ID de l'événement est obligatoire")
    private UUID eventId;

}