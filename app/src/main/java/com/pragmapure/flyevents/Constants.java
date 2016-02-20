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

    public static final int PERMISSIONS_QUERY = 5;



    public static final long minTime = 0;
    public static final float minDistance = 0;
    /*SERVER*/

    public static final String SERVER_URL = "http://37.139.6.22/";

}
