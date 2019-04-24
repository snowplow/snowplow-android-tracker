package com.snowplowanalytics.snowplow.tracker.tracker;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

import com.snowplowanalytics.snowplow.tracker.Tracker;
import com.snowplowanalytics.snowplow.tracker.constants.TrackerConstants;
import com.snowplowanalytics.snowplow.tracker.events.SelfDescribing;
import com.snowplowanalytics.snowplow.tracker.payload.SelfDescribingJson;

import java.util.Calendar;

import static com.snowplowanalytics.snowplow.tracker.constants.TrackerConstants.INSTALLED_BEFORE;
import static com.snowplowanalytics.snowplow.tracker.constants.TrackerConstants.INSTALL_TIMESTAMP;

public class InstallTracker {
    /**
     * Class used to keep track of install state of app.
     * If a file does not exist, the tracker will send an `application_install` event.
     */
    private static String TAG = InstallTracker.class.getSimpleName();
    private Boolean isNewInstall;
    private Context context;

    private SharedPreferences sharedPreferences;

    public InstallTracker(Context context) {
        this.context = context;
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
            long installTimestamp = getInstallTimestamp();
            if (isNewInstall) {
                sendInstallEvent();
                if (installTimestamp != 0) {
                    clearInstallTimestamp();
                }
            } else if (installTimestamp != 0) {
                sendInstallEvent(installTimestamp);
                clearInstallTimestamp();
            }
        }
    }

    private long getInstallTimestamp() {
        return sharedPreferences.getLong(INSTALL_TIMESTAMP, 0);
    }

    private void clearInstallTimestamp() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(INSTALL_TIMESTAMP);
        editor.apply();
    }

    private void sendInstallEvent() {
        SelfDescribing event = SelfDescribing.builder()
                .eventData(new SelfDescribingJson(TrackerConstants.SCHEMA_APPLICATION_INSTALL))
                .build();
        Tracker.instance().track(event);
    }

    private void sendInstallEvent(long installTimestamp) {
        SelfDescribing event = SelfDescribing.builder()
                .eventData(new SelfDescribingJson(TrackerConstants.SCHEMA_APPLICATION_INSTALL))
                .deviceCreatedTimestamp(installTimestamp)
                .build();
        Tracker.instance().track(event);
    }
}
