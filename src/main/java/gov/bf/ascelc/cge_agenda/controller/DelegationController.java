package gov.bf.ascelc.cge_agenda.controller;

import gov.bf.ascelc.cge_agenda.service.impl.DelegationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static gov.bf.ascelc.cge_agenda.utils.ApiUrls.*;

/**
 * Endpoints publics (non authentifiés) cliqués depuis l'email de délégation.
 * Retourne directement une page HTML de confirmation — pas de redirection vers le
 * frontend, pour que ça fonctionne même sans session applicative.
 */
@RestController
@RequestMapping(DELEGATION_ROOT_URL)
@RequiredArgsConstructor
public class DelegationController {

    private final DelegationService delegationService;

    @GetMapping(value = DELEGATION_ACCEPTER, produces = MediaType.TEXT_HTML_VALUE)
    public String accepter(@PathVariable String token) {
        DelegationService.Resultat resultat = delegationService.accepter(token);
        return switch (resultat) {
            case ACCEPTEE -> page("Délégation confirmée", "✅",
                    "Merci, votre participation en tant que délégué(e) est confirmée.");
            case DEJA_TRAITEE, DECLINEE -> page("Déjà traité", "ℹ️",
                    "Cette délégation a déjà fait l'objet d'une réponse.");
        };
    }

    @GetMapping(value = DELEGATION_DECLINER, produces = MediaType.TEXT_HTML_VALUE)
    public String decliner(@PathVariable String token) {
        DelegationService.Resultat resultat = delegationService.decliner(token);
        return switch (resultat) {
            case DECLINEE -> page("Délégation déclinée", "❌",
                    "Votre refus a bien été enregistré. Le créateur de l'événement a été prévenu.");
            case DEJA_TRAITEE, ACCEPTEE -> page("Déjà traité", "ℹ️",
                    "Cette délégation a déjà fait l'objet d'une réponse.");
        };
    }

    @ExceptionHandler(ResponseStatusException.class)
    @ResponseStatus(HttpStatus.OK)
    public String handleInvalidToken(ResponseStatusException ex) {
        return page("Lien invalide", "⚠️", ex.getReason() != null ? ex.getReason() : "Ce lien n'est plus valide.");
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
