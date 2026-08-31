package gov.bf.ascelc.cge_agenda.service;

import gov.bf.ascelc.cge_agenda.dto.MembreEspaceDto;
import gov.bf.ascelc.cge_agenda.enums.MembreEspaceRole;

import java.util.List;
import java.util.UUID;

public interface MembreEspaceService {

    /**
     * Membres de l'espace — appelable seulement par le propriétaire de l'espace (ou ADMIN).
     */
    List<MembreEspaceDto> getMembres(UUID espaceId);

    /**
     * Ajoute un gestionnaire à l'espace : crée la ligne en statut INVITE et envoie un
     * email avec un lien à usage unique. Pas de refus possible — le lien fait passer
     * directement à ACTIF (prise de connaissance, pas un vote).
     */
    MembreEspaceDto ajouterMembre(UUID espaceId, String membreEmail, String membreNom,
                                   MembreEspaceRole role, String invitedByEmail);

    void retirerMembre(UUID espaceId, UUID membreEspaceId);

    enum RejoindreResultat { ACTIVE, DEJA_ACTIF, INVALIDE }

    /**
     * Traite le clic sur le lien d'invitation : vérifie la signature ET le statut en
     * base (gère la révocation si le chef a retiré le membre avant qu'il ne clique),
     * puis passe INVITE → ACTIF. Aucun refus possible par conception.
     */
    RejoindreResultat rejoindre(String token);
}
