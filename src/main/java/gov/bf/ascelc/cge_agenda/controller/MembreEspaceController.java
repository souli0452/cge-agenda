package gov.bf.ascelc.cge_agenda.controller;

import gov.bf.ascelc.cge_agenda.dto.EspaceDto;
import gov.bf.ascelc.cge_agenda.dto.MembreEspaceDto;
import gov.bf.ascelc.cge_agenda.enums.MembreEspaceRole;
import gov.bf.ascelc.cge_agenda.service.EspaceService;
import gov.bf.ascelc.cge_agenda.service.MembreEspaceService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static gov.bf.ascelc.cge_agenda.utils.ApiUrls.*;

/**
 * Gestion des gestionnaires (secrétaire, protocole) d'un espace — réservée au
 * propriétaire de cet espace (ou ADMIN), vérifié en service (MembreEspaceServiceImpl).
 */
@RestController
@RequestMapping(ESPACE_ROOT_URL)
@RequiredArgsConstructor
public class MembreEspaceController {

    private final MembreEspaceService membreEspaceService;
    private final EspaceService espaceService;

    @GetMapping(ESPACE_MES_ESPACES)
    public ResponseEntity<List<EspaceDto>> mesEspaces() {
        return ResponseEntity.ok(espaceService.mesEspaces());
    }

    @GetMapping(ESPACE_MEMBRES)
    public ResponseEntity<List<MembreEspaceDto>> getMembres(@PathVariable UUID espaceId) {
        return ResponseEntity.ok(membreEspaceService.getMembres(espaceId));
    }

    @PostMapping(ESPACE_MEMBRES)
    public ResponseEntity<MembreEspaceDto> ajouterMembre(
            @PathVariable UUID espaceId,
            @org.springframework.web.bind.annotation.RequestBody AjouterMembreRequest req,
            Authentication authentication
    ) {
        String invitedBy = (authentication != null && authentication.getPrincipal() instanceof Jwt jwt)
                ? jwt.getClaim("email") : null;
        MembreEspaceDto dto = membreEspaceService.ajouterMembre(
                espaceId, req.getMembreEmail(), req.getMembreNom(), req.getRole(), invitedBy);
        return new ResponseEntity<>(dto, HttpStatus.CREATED);
    }

    @DeleteMapping(ESPACE_MEMBRE_BY_ID)
    public ResponseEntity<Void> retirerMembre(@PathVariable UUID espaceId, @PathVariable UUID membreEspaceId) {
        membreEspaceService.retirerMembre(espaceId, membreEspaceId);
        return ResponseEntity.noContent().build();
    }

    @Getter
    @Setter
    public static class AjouterMembreRequest {
        @NotBlank
        private String membreEmail;
        private String membreNom;
        @NotNull
        private MembreEspaceRole role;
    }
}
