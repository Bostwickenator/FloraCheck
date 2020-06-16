package org.bostwickenator.floracheck;

import android.content.Context;

class SettingsManager {
    public static final String PREFS_NAME = "prefs";
    public static final String SETTING_METRIC_UNITS = "humidity";

    public static int getHumidity(Context mContext) {
        return mContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getInt(SETTING_METRIC_UNITS, 50);
    }
}
