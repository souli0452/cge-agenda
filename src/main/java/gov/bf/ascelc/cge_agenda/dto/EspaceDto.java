package gov.bf.ascelc.cge_agenda.dto;

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
public class EspaceDto {
    private UUID id;
    private String nom;
    private String chefEmail;
    private String chefNom;
    private LocalDateTime createdAt;
}
