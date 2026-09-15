package gov.bf.ascelc.cge_agenda.controller;

import gov.bf.ascelc.cge_agenda.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static gov.bf.ascelc.cge_agenda.utils.ApiUrls.AUTH_ROOT_URL;
import static gov.bf.ascelc.cge_agenda.utils.ApiUrls.TRACK_LOGIN;

@RestController
@RequestMapping(AUTH_ROOT_URL)
@RequiredArgsConstructor
public class AuthController {

    private final AuditService auditService;

    /**
     * Enregistre une connexion réussie dans le journal d'audit.
     * Appelé par le frontend juste après l'authentification Keycloak.
     */
    @PostMapping(TRACK_LOGIN)
    public ResponseEntity<Void> trackLogin(HttpServletRequest request) {
        auditService.logAction("CONNEXION", null, null, null, null, request);
        return ResponseEntity.noContent().build();
    }
}
