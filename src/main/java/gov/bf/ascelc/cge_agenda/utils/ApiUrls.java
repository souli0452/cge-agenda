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
    public static final String CREATE_EVENT = EVENT_ROOT_URL + "/create";
    public static final String UPDATE_EVENT = EVENT_ROOT_URL + "/update/{id}";
    public static final String DELETE_EVENT = EVENT_ROOT_URL + "/delete/{id}";
    public static final String GET_EVENT_BY_ID = EVENT_ROOT_URL + "/get/{id}";
    public static final String GET_ALL_EVENT = EVENT_ROOT_URL + "/all";

    // Actions sur événements
    public static final String CANCEL_EVENT = "/cancel/{id}";
    public static final String POSTPONE_EVENT = "/postpone/{id}";

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
    public static final String GET_PARTICIPANTS_BY_TYPE = "/type/{type}";

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
}