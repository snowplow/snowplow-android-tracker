package com.snowplowanalytics.snowplow.tracker.tracker;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.snowplowanalytics.snowplow.tracker.Executor;
import com.snowplowanalytics.snowplow.tracker.Tracker;
import com.snowplowanalytics.snowplow.tracker.constants.Parameters;
import com.snowplowanalytics.snowplow.tracker.constants.TrackerConstants;
import com.snowplowanalytics.snowplow.tracker.events.SelfDescribing;
import com.snowplowanalytics.snowplow.tracker.payload.SelfDescribingJson;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

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
    private Future infoFuture;

    private SharedPreferences sharedPreferences;

    public InstallTracker(Context context) {
        this.context = context;
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        if (sharedPreferences.getString(INSTALLED_BEFORE, null) == null) {
            // mark the install if there's no value
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(INSTALLED_BEFORE, "YES");
            editor.putLong(INSTALL_TIMESTAMP, Calendar.getInstance().getTimeInMillis());
            editor.apply();
            // since the value was missing in sharedPreferences, we're assuming this is a new install
            this.isNewInstall = true;
        } else {
            // if there's an INSTALLED_BEFORE record in sharedPreferences - someone has been there!
            this.isNewInstall = false;
        }
        this.infoFuture = null;
        checkInstall();
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

    private void checkInstall() {
        this.infoFuture = Executor.futureCallable(new Callable<Map<String, Object>>() {
            @Override
            public Map<String,Object> call() {
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
                return getInstallValues();
            }
        });
    }


    private Map<String, Object> getInstallValues() {
        Map<String, Object> installValues = new HashMap<>();
        installValues.put(Parameters.INSTALL_STATUS, isNewInstall);
        return installValues;
    }
}
