package org.nevadabike.renotracks;

import android.content.ContentResolver;
import android.provider.Settings.System;

public class Common
{
	public static final String API_URL = "http://rtstage.nevadabike.org/post/";
	public static final String TIMESTAMP_FORMAT = "yyyy-MM-dd HH:mm:ss";

	public static final float METER_TO_MILE = 0.0006212f;

    public static String getDeviceId(ContentResolver contentResolver)
    {
        String androidId = System.getString(contentResolver, System.ANDROID_ID);
        String androidBase = "androidDeviceId_";

        if (androidId == null) { // This happens when running in the Emulator
            final String emulatorId = "android-RunningAsTestingDeleteMe";
            return emulatorId;
        }
        String deviceId = androidBase.concat(androidId);
        return deviceId;
    }

    public static double distanceToCals(double meters)
    {
    	return 49 * meters * METER_TO_MILE - 1.69;
    }

    public static double distanceToCO2(double meters)
    {
    	return 0.93 * meters * METER_TO_MILE;
    }
}
