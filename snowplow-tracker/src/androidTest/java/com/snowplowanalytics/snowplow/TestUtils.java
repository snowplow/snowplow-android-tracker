package com.snowplowanalytics.snowplow;

import android.content.Context;
import android.content.SharedPreferences;

import com.snowplowanalytics.snowplow.internal.constants.Parameters;
import com.snowplowanalytics.snowplow.internal.constants.TrackerConstants;
import com.snowplowanalytics.snowplow.internal.utils.Util;

public class TestUtils {

    public static void createSessionSharedPreferences(Context context, String namespaceId) {
        String sessionVarsName = TrackerConstants.SNOWPLOW_SESSION_VARS + "_" + namespaceId;
        SharedPreferences sharedPreferences = context.getSharedPreferences(sessionVarsName, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(Parameters.SESSION_USER_ID, Util.getUUIDString());
        editor.putString(Parameters.SESSION_ID, Util.getUUIDString());
        editor.putInt(Parameters.SESSION_INDEX, 0);
        editor.commit();
    }
}
