package gov.bf.ascelc.cge_agenda.dto;

import gov.bf.ascelc.cge_agenda.abstracts.AuditEntityDto;
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
public class FileDto extends AuditEntityDto {

    private String fileName;

    private String fileLink;

    private String fileType;

    private UUID eventId;
}
