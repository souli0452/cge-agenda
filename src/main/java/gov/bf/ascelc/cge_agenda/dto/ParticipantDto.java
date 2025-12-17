package gov.bf.ascelc.cge_agenda.dto;

import gov.bf.ascelc.cge_agenda.abstracts.AuditEntityDto;
import gov.bf.ascelc.cge_agenda.enums.ParticipantType;
import jakarta.persistence.Column;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@Getter
@Setter
public class ParticipantDto extends AuditEntityDto {

    @NotBlank(message = "Le nom est obligatoire")
    private String lastName;

    @NotBlank(message = "Le prénom est obligatoire")
    private String firstName;

    @NotBlank(message = "L'email est obligatoire")
    private String email;

    private String  phoneNumber;

    private String  jobTitle;

    private String  organization;

    private ParticipantType typeParticipant;

}