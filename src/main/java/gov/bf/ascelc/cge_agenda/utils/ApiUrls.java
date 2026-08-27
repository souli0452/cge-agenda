package gov.bf.ascelc.cge_agenda.utils;

public class ApiUrls {

    /* Base URLs */
    public static final String CGE_AGENDA_ROOT_URL = "api/v1/cge-agenda";

    /* ========================================== */
    /* STATS URLs                                 */
    /* ========================================== */
    public static final String STATS_ROOT_URL = CGE_AGENDA_ROOT_URL + "/stats";


    /* ========================================== */
    /* EVENT URLs                                 */
    /* ========================================== */
    public static final String EVENT_ROOT_URL = CGE_AGENDA_ROOT_URL + "/event";

    // CRUD basique
    public static final String CREATE_EVENT = "/create";
    public static final String UPDATE_EVENT = "/update/{id}";
    public static final String DELETE_EVENT = "/delete/{id}";
    public static final String GET_EVENT_BY_ID = "/get/{id}";
    public static final String GET_ALL_EVENT = "/all";

    // Actions sur événements
    public static final String CANCEL_EVENT = "/cancel/{id}";
    public static final String POSTPONE_EVENT = "/postpone/{id}";

    // Corbeille
    public static final String GET_EVENT_CORBEILLE   = "/corbeille";
    public static final String RESTORE_EVENT         = "/{id}/restaurer";
    public static final String DELETE_EVENT_PERMANENT= "/{id}/supprimer-definitivement";

    // Workflow de validation CGE
    public static final String SUBMIT_EVENT          = "/submit/{id}";
    public static final String VALIDATE_EVENT        = "/validate/{id}";
    public static final String REJECT_EVENT          = "/reject/{id}";
    public static final String REQUEST_CHANGES_EVENT = "/request-changes/{id}";
    public static final String DELEGATE_EVENT        = "/delegate/{id}";
    public static final String ADD_OBSERVATION_EVENT = "/{id}/observation";
    public static final String SAVE_COMPTE_RENDU     = "/{id}/compte-rendu";

    // Recherche et calendrier
    public static final String SEARCH_EVENTS = "/search";
    public static final String CALENDAR_MONTHLY = "/calendar/{year}/{month}";
    public static final String EVENTS_BY_PERIOD = "/period";

    // Gestion des participants d'un événement
    public static final String ADD_PARTICIPANT_TO_EVENT = "/{eventId}/participants";
    public static final String REMOVE_PARTICIPANT_FROM_EVENT = "/{eventId}/participants/{participantId}";
    public static final String GET_EVENT_PARTICIPANTS = "/{eventId}/participants";
    public static final String IMPORT_PARTICIPANTS = "/{eventId}/participants/import";
    //public static final String GENERATE_ATTENDANCE_SHEET = "/{eventId}/attendance-sheet";
    public static final String GENERATE_ATTENDANCE_SHEET = "/attendance-sheet/{id}";

    /* ========================================== */
    /* PARTICIPANT URLs                           */
    /* ========================================== */
    public static final String PARTICIPANT_ROOT_URL = CGE_AGENDA_ROOT_URL + "/participant";

    public static final String CREATE_PARTICIPANT = "/create";
    public static final String UPDATE_PARTICIPANT = "/update/{id}";
    public static final String DELETE_PARTICIPANT = "/delete/{id}";
    public static final String GET_PARTICIPANT_BY_ID = "/{id}";
    public static final String GET_ALL_PARTICIPANT = "/all";
    public static final String SEARCH_PARTICIPANTS = "/search";
    public static final String AUTOCOMPLETE_PARTICIPANTS = "/autocomplete";
    public static final String GET_PARTICIPANTS_BY_TYPE = "/type/{type}";
    public static final String GET_PARTICIPANTS_PAGED = "/paged";
    public static final String GET_PARTICIPANT_CORBEILLE = "/corbeille";
    public static final String RESTORE_PARTICIPANT = "/{id}/restore";
    public static final String DELETE_PARTICIPANT_PERMANENT = "/{id}/permanent";

    /* ========================================== */
    /* SCHEDULE URLs                              */
    /* ========================================== */
    public static final String SCHEDULE_ROOT_URL = CGE_AGENDA_ROOT_URL + "/schedule";

    public static final String CREATE_SCHEDULE = "/create";
    public static final String UPDATE_SCHEDULE = "/update/{id}";
    public static final String DELETE_SCHEDULE = "/delete/{id}";
    public static final String GET_SCHEDULES_BY_EVENT = "/event/{eventId}";

    /* ========================================== */
    /* FILE URLs                                  */
    /* ========================================== */
    public static final String FILE_ROOT_URL = CGE_AGENDA_ROOT_URL + "/file";

    public static final String UPLOAD_FILE = "/upload";
    public static final String DOWNLOAD_FILE = "/download/{id}";
    public static final String DELETE_FILE = "/delete/{id}";
    public static final String GET_FILES_BY_EVENT = "/event/{eventId}";

    /* ========================================== */
    /* AUTH URLs                                  */
    /* ========================================== */
    public static final String AUTH_ROOT_URL = CGE_AGENDA_ROOT_URL + "/auth";

    public static final String TRACK_LOGIN = "/track-login";

    /* ========================================== */
    /* AUDIT URLs                                 */
    /* ========================================== */
    public static final String AUDIT_ROOT_URL = CGE_AGENDA_ROOT_URL + "/audit";

    public static final String AUDIT_PAGED = "/paged";
    public static final String AUDIT_RECENTLY_ACTIVE = "/recently-active";

    /* ========================================== */
    /* ADMIN (utilisateurs / rôles Keycloak)      */
    /* ========================================== */
    public static final String ADMIN_ROOT_URL = CGE_AGENDA_ROOT_URL + "/admin";

    public static final String ADMIN_USERS               = "/users";
    public static final String ADMIN_USER_BY_ID           = "/users/{id}";
    public static final String ADMIN_USER_STATUS          = "/users/{id}/status";
    public static final String ADMIN_USER_RESET_PASSWORD  = "/users/{id}/reset-password";
    public static final String ADMIN_ROLES                = "/roles";
    public static final String ADMIN_ROLE_BY_NAME         = "/roles/{roleName}";
    public static final String ADMIN_USER_ROLES           = "/users/{userId}/roles";
    public static final String ADMIN_USER_ROLE_BY_NAME    = "/users/{userId}/roles/{roleName}";

    // Configuration de l'organisation (identité, sujets d'emails)
    public static final String ADMIN_CONFIG               = "/config";
    public static final String ADMIN_CONFIG_PREVIEW        = "/config/preview/{templateKey}";

    // Configuration des rappels automatiques
    public static final String ADMIN_SCHEDULER            = "/scheduler";
    public static final String ADMIN_SCHEDULER_RUN_NOW     = "/scheduler/run-now";

    // Sauvegardes de la base de données
    public static final String ADMIN_BACKUP                = "/backup";
    public static final String ADMIN_BACKUP_CONFIG          = "/backup/config";
    public static final String ADMIN_BACKUP_BY_FILENAME     = "/backup/{filename}";
    public static final String ADMIN_BACKUP_RESTORE         = "/backup/restore/{filename}";
    public static final String ADMIN_BACKUP_DOWNLOAD        = "/backup/download/{filename}";
    public static final String ADMIN_BACKUP_CORBEILLE       = "/backup/corbeille";
    public static final String ADMIN_BACKUP_CORBEILLE_RESTORE = "/backup/corbeille/{filename}/restore";
    public static final String ADMIN_BACKUP_CORBEILLE_DELETE   = "/backup/corbeille/{filename}";

    /* ========================================== */
    /* SETTINGS URLs (préférences utilisateur)    */
    /* ========================================== */
    public static final String SETTINGS_ROOT_URL = CGE_AGENDA_ROOT_URL + "/settings";
}