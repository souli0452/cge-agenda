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

/**
 * DTO pour créer un participant lors de la création d'un événement
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantDto extends AuditEntityDto {
    @NotBlank(message = "Le nom est obligatoire")
    private String lastName;
    @NotBlank(message = "Le prénom est obligatoire")
    private String firstName;

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "Format d'email invalide")
    private String email;

    private String phoneNumber;
    private String jobTitle;

    private String structure;

    @NotNull(message = "Le type de participant est obligatoire")
    private ParticipantType participantType;

    private boolean deleted;
    private java.time.LocalDateTime deletedAt;
    private String deletedBy;
}
