package gov.bf.ascelc.cge_agenda.utils;

public class ApiUrls {

    /* Base urls */
    public static final String CGE_AGENDA_ROOT_URL="api/v1/cge-agenda";

    /* Event urls */

    public static final String EVENT_ROOT_URL = CGE_AGENDA_ROOT_URL + "/event";
    public static final String CREATE_EVENT = "/create";
    public static final String DELETE_EVENT = "/delete/{id}";
    public static final String GET_ALL_EVENT = "/all";
    public static final String UPDATE_EVENT = "/update";
    public static final String GET_EVENT_BY_ID = "/get/{id}";

    /* Participant URLs */
    public static final String PARTICIPANT_ROOT_URL = CGE_AGENDA_ROOT_URL + "/participant";
    public static final String CREATE_PARTICIPANT = "/create";
    public static final String DELETE_PARTICIPANT = "/delete/{id}";
    public static final String GET_ALL_PARTICIPANT = "/all";
    public static final String UPDATE_PARTICIPANT = "/update/{id}";
    public static final String GET_PARTICIPANT_BY_ID = "/{id}";

    /* Schedule URLs */
    public static final String SCHEDULE_ROOT_URL = CGE_AGENDA_ROOT_URL + "/schedule";
    public static final String CREATE_SCHEDULE = "/create";
    public static final String DELETE_SCHEDULE = "/delete/{id}";
    public static final String GET_SCHEDULES_BY_EVENT = "/event/{eventId}";

    /* File URLs */
    public static final String FILE_ROOT_URL = CGE_AGENDA_ROOT_URL + "/file";
    public static final String UPLOAD_FILE = "/upload";
    public static final String DOWNLOAD_FILE = "/download/{id}";
    public static final String DELETE_FILE = "/delete/{id}";
    public static final String GET_FILES_BY_EVENT = "/event/{eventId}";
}


