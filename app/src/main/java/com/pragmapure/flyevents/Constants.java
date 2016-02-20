package com.pragmapure.flyevents;

/**
 * Created by xiscosastre on 20/02/16.
 */
public final class Constants {

    private Constants() {
        // restrict instantiation
    }

    /*SHARED PREFERENCES KEYS*/
    public static final String SP_FE = "spfe";
    public static final String IMEI_KEY = "imeikey";
    public static final String REGISTERED_KEY = "regkey";
    public static final String GPS_LAT_KEY = "gpslat";
    public static final String GPS_LONG_KEY = "gpslong";
    public static final String EVENT_KEY = "eventk";
    public static final String EVENTS_FOUND_KEY = "eventsfound";
    public static final String EVENTS_NOTIFICATION = "eventsfound";
    public static final String DATEON_KEY = "dateon";
    public static final String DATEOFF_KEY = "dateoff";
    public static final String DATE_FORMAT_SERVER = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    public static final int PERMISSIONS_QUERY = 5;



    public static final long minTime = 5000;
    public static final float minDistance = 0;
    public static final int TIME_GPS_SEARCH = 10000;
    /*SERVER*/

    public static final String SERVER_URL = "http://37.139.6.22/";
    public static final String CREATE_USER = SERVER_URL + "api/users/";
    public static final String ACTUAL_EVENT = SERVER_URL + "api/users/actual_event/";
    public static final String SEARCH_EVENTS = SERVER_URL + "api/events/search/";
    public static final String UPLOAD_URL = SERVER_URL + "api/photos/";
    public static final String WEB_EVENTS_URL = SERVER_URL + "events/";

}
