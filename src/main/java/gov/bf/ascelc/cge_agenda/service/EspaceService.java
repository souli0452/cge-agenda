package gov.bf.ascelc.cge_agenda.service;

import gov.bf.ascelc.cge_agenda.dto.EspaceDto;

import java.util.List;
import java.util.UUID;

public interface EspaceService {

    List<EspaceDto> getAll();

    EspaceDto create(String nom, String chefEmail, String chefNom);

    EspaceDto update(UUID id, String nom, String chefEmail, String chefNom);

    /**
     * Désactive/réactive un espace. Un espace désactivé ne peut plus accueillir de nouveaux
     * événements (voir peutCreerDans) mais conserve tout son historique — pas de suppression
     * physique possible, pour ne jamais perdre les données qui y sont rattachées.
     */
    void setActif(UUID id, boolean actif);

    /**
     * Espaces accessibles à l'email donné : celui qu'il possède (le cas échéant) +
     * ceux où il est membre ACTIF. Fondement du cloisonnement des événements.
     */
    List<UUID> espacesAccessibles(String email);

    boolean estProprietaire(UUID espaceId, String email);

    /**
     * Peut créer un événement dans cet espace = en est propriétaire OU y est membre
     * ACTIF (secrétaire ou protocole) — règle unique, aucune exception codée par chef.
     */
    boolean peutCreerDans(UUID espaceId, String email);

    /**
     * Espaces accessibles à l'utilisateur courant (résolus, pour l'affichage — sélecteur
     * d'espace côté front notamment).
     */
    List<EspaceDto> mesEspaces();
}
