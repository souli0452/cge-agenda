package gov.bf.ascelc.cge_agenda.controller;

import gov.bf.ascelc.cge_agenda.service.MembreEspaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static gov.bf.ascelc.cge_agenda.utils.ApiUrls.*;

/**
 * Endpoint public (non authentifié) cliqué depuis l'email d'invitation à devenir
 * gestionnaire d'un espace. Une seule action possible : prendre connaissance (pas de
 * refus) — cf. calqué sur DelegationController pour le style de page de confirmation.
 */
@RestController
@RequestMapping(ESPACE_MEMBRE_ROOT_URL)
@RequiredArgsConstructor
public class EspaceMembreController {

    private final MembreEspaceService membreEspaceService;

    @GetMapping(value = ESPACE_MEMBRE_REJOINDRE, produces = MediaType.TEXT_HTML_VALUE)
    public String rejoindre(@PathVariable String token) {
        MembreEspaceService.RejoindreResultat resultat = membreEspaceService.rejoindre(token);
        return switch (resultat) {
            case ACTIVE -> page("Espace rejoint", "✅",
                    "Vous êtes désormais gestionnaire de cet espace agenda. Vous pouvez vous connecter à l'application.");
            case DEJA_ACTIF -> page("Déjà actif", "ℹ️",
                    "Vous êtes déjà gestionnaire de cet espace.");
            case INVALIDE -> page("Lien invalide", "⚠️",
                    "Ce lien n'est plus valide (expiré ou révoqué).");
        };
    }

    private String page(String title, String icon, String message) {
        return "<!DOCTYPE html><html lang=\"fr\"><head><meta charset=\"UTF-8\"><title>" + title + "</title>"
                + "<style>body{font-family:'Segoe UI',Tahoma,Geneva,Verdana,sans-serif;background:#f0f4f0;"
                + "display:flex;align-items:center;justify-content:center;min-height:100vh;margin:0;}"
                + ".card{background:#fff;border-radius:16px;box-shadow:0 8px 32px rgba(0,0,0,.12);"
                + "padding:40px;max-width:420px;text-align:center;}"
                + ".icon{font-size:48px;margin-bottom:16px;}"
                + "h1{font-size:20px;color:#333;margin-bottom:12px;}"
                + "p{color:#666;font-size:14px;line-height:1.6;}"
                + "</style></head><body><div class=\"card\">"
                + "<div class=\"icon\">" + icon + "</div><h1>" + title + "</h1><p>" + message + "</p>"
                + "</div></body></html>";
    }
}
