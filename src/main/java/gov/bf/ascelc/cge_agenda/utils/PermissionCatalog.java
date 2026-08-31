package gov.bf.ascelc.cge_agenda.utils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Catalogue fixe des permissions que l'application sait vérifier — la LISTE des clés
 * possibles est du code (les fonctionnalités de l'appli sont fixes), mais QUEL rôle a
 * QUELLE clé est dynamique (table role_permission, éditable en admin). Utilisé pour le
 * seed de la migration V13 et par l'écran admin pour afficher des libellés lisibles.
 */
public final class PermissionCatalog {

    private PermissionCatalog() {}

    public static final String EVENT_VIEW = "EVENT_VIEW";
    public static final String EVENT_CREATE = "EVENT_CREATE";
    public static final String EVENT_EDIT = "EVENT_EDIT";
    public static final String EVENT_DELETE = "EVENT_DELETE";
    public static final String EVENT_CANCEL_POSTPONE = "EVENT_CANCEL_POSTPONE";
    public static final String EVENT_VALIDATE = "EVENT_VALIDATE";
    public static final String EVENT_REJECT = "EVENT_REJECT";
    public static final String EVENT_REQUEST_CHANGES = "EVENT_REQUEST_CHANGES";
    public static final String EVENT_DELEGATE = "EVENT_DELEGATE";
    public static final String EVENT_OBSERVATION = "EVENT_OBSERVATION";
    public static final String EVENT_DEMANDER_DELEGATION = "EVENT_DEMANDER_DELEGATION";
    public static final String EVENT_EXPORT_PDF = "EVENT_EXPORT_PDF";
    public static final String STATS_VIEW = "STATS_VIEW";
    public static final String AUDIT_VIEW = "AUDIT_VIEW";
    public static final String ADMIN_CONFIG = "ADMIN_CONFIG";
    public static final String ADMIN_USERS = "ADMIN_USERS";
    public static final String ESPACE_MANAGE = "ESPACE_MANAGE";

    /** Libellés affichés dans l'écran admin "Rôles & permissions". */
    public static final Map<String, String> LABELS = new LinkedHashMap<>();
    static {
        LABELS.put(EVENT_VIEW, "Voir les événements");
        LABELS.put(EVENT_CREATE, "Créer un événement");
        LABELS.put(EVENT_EDIT, "Modifier un événement");
        LABELS.put(EVENT_DELETE, "Supprimer définitivement un événement");
        LABELS.put(EVENT_CANCEL_POSTPONE, "Annuler / reporter un événement");
        LABELS.put(EVENT_VALIDATE, "Valider un événement");
        LABELS.put(EVENT_REJECT, "Rejeter un événement");
        LABELS.put(EVENT_REQUEST_CHANGES, "Demander des modifications");
        LABELS.put(EVENT_DELEGATE, "Déléguer la participation");
        LABELS.put(EVENT_OBSERVATION, "Ajouter une observation");
        LABELS.put(EVENT_DEMANDER_DELEGATION, "Demander une délégation");
        LABELS.put(EVENT_EXPORT_PDF, "Exporter en PDF");
        LABELS.put(STATS_VIEW, "Voir les statistiques");
        LABELS.put(AUDIT_VIEW, "Voir le journal d'audit");
        LABELS.put(ADMIN_CONFIG, "Gérer la configuration système");
        LABELS.put(ADMIN_USERS, "Gérer les comptes utilisateurs");
        LABELS.put(ESPACE_MANAGE, "Créer/gérer les espaces");
    }
}
