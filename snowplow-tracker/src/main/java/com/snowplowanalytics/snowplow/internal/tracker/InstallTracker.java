package com.snowplowanalytics.snowplow.internal.tracker;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import com.snowplowanalytics.snowplow.event.SelfDescribing;
import com.snowplowanalytics.snowplow.internal.constants.TrackerConstants;
import com.snowplowanalytics.snowplow.internal.utils.NotificationCenter;
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import static com.snowplowanalytics.snowplow.internal.constants.TrackerConstants.INSTALLED_BEFORE;
import static com.snowplowanalytics.snowplow.internal.constants.TrackerConstants.INSTALL_TIMESTAMP;

/**
 * Class used to keep track of install state of app.
 * If a file does not exist, the tracker will send an `application_install` event.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class InstallTracker {
    private static String TAG = InstallTracker.class.getSimpleName();

    private Boolean isNewInstall;
    private SharedPreferences sharedPreferences;

    private static InstallTracker sharedInstance;

    @NonNull
    public synchronized static InstallTracker getInstance(@NonNull Context context) {
        if (sharedInstance == null) {
            sharedInstance = new InstallTracker(context);
        }
        return sharedInstance;
    }

    private InstallTracker(@NonNull Context context) {
        new SharedPreferencesTask().execute(context);
    }

    private class SharedPreferencesTask extends AsyncTask<Context, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Context... contexts) {
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(contexts[0]);
            if (sharedPreferences.getString(INSTALLED_BEFORE, null) == null) {
                // mark the install if there's no value
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(INSTALLED_BEFORE, "YES");
                editor.putLong(INSTALL_TIMESTAMP, Calendar.getInstance().getTimeInMillis());
                editor.apply();
                // since the value was missing in sharedPreferences, we're assuming this is a new install
                isNewInstall = true;
            } else {
                // if there's an INSTALLED_BEFORE record in sharedPreferences - someone has been there!
                isNewInstall = false;
            }
            return isNewInstall;
        }

        @Override
        protected void onPostExecute(Boolean isNewInstall) {
            long installTimestamp = sharedPreferences.getLong(INSTALL_TIMESTAMP, 0);
            // We send the installEvent if it's a new installed app but in case the tracker hasn't been able
            // to send the event before we can retry checking if INSTALL_TIMESTAMP was already removed.
            if (!isNewInstall && installTimestamp <= 0) {
                return;
            }
            sendInstallEvent(installTimestamp);
            // clear install timestamp
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.remove(INSTALL_TIMESTAMP);
            editor.commit();
        }
    }

    private void sendInstallEvent(long installTimestamp) {
        SelfDescribing event = new SelfDescribing(new SelfDescribingJson(TrackerConstants.SCHEMA_APPLICATION_INSTALL));
        if (installTimestamp > 0) {
            event.trueTimestamp(installTimestamp);
        }
        Map<String, Object> notificationData = new HashMap<String, Object>();
        notificationData.put("event", event);
        NotificationCenter.postNotification("SnowplowInstallTracking", notificationData);
    }
}
