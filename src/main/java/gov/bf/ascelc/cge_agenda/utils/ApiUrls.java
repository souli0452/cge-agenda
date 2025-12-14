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

}
