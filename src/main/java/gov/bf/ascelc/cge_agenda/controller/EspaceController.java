package gov.bf.ascelc.cge_agenda.controller;

import gov.bf.ascelc.cge_agenda.dto.EspaceDto;
import gov.bf.ascelc.cge_agenda.service.EspaceService;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static gov.bf.ascelc.cge_agenda.utils.ApiUrls.*;

/**
 * Création/suppression des espaces — réservé à ADMIN (un espace par chef, l'admin
 * décide qui est chef exactement comme il gère déjà les comptes Keycloak).
 */
@RestController
@RequestMapping(ADMIN_ROOT_URL)
@RequiredArgsConstructor
public class EspaceController {

    private final EspaceService espaceService;

    @GetMapping(ADMIN_ESPACES)
    public ResponseEntity<List<EspaceDto>> getAll() {
        return ResponseEntity.ok(espaceService.getAll());
    }

    @PostMapping(ADMIN_ESPACES)
    public ResponseEntity<EspaceDto> create(@org.springframework.web.bind.annotation.RequestBody CreateEspaceRequest req) {
        return new ResponseEntity<>(espaceService.create(req.getNom(), req.getChefEmail(), req.getChefNom()), HttpStatus.CREATED);
    }

    @DeleteMapping(ADMIN_ESPACE_BY_ID)
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        espaceService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Getter
    @Setter
    public static class CreateEspaceRequest {
        @NotBlank
        private String nom;
        @NotBlank
        private String chefEmail;
        private String chefNom;
    }
}
