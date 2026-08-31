package gov.bf.ascelc.cge_agenda.dto;

import gov.bf.ascelc.cge_agenda.enums.MembreEspaceRole;
import gov.bf.ascelc.cge_agenda.enums.MembreEspaceStatut;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MembreEspaceDto {
    private UUID id;
    private UUID espaceId;
    private String membreEmail;
    private String membreNom;
    private MembreEspaceRole role;
    private MembreEspaceStatut statut;
    private LocalDateTime invitedAt;
    private LocalDateTime activatedAt;
}
