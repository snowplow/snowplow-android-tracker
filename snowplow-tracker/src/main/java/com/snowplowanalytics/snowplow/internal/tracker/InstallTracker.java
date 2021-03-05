package com.snowplowanalytics.snowplow.internal.tracker;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import com.snowplowanalytics.snowplow.event.SelfDescribing;
import com.snowplowanalytics.snowplow.internal.constants.Parameters;
import com.snowplowanalytics.snowplow.internal.constants.TrackerConstants;
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import static com.snowplowanalytics.snowplow.internal.constants.TrackerConstants.INSTALLED_BEFORE;
import static com.snowplowanalytics.snowplow.internal.constants.TrackerConstants.INSTALL_TIMESTAMP;

import static com.snowplowanalytics.snowplow.internal.utils.Util.addToMap;

@RestrictTo(RestrictTo.Scope.LIBRARY)
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
        try {
            Tracker.instance().track(event);
        } catch(IllegalStateException e) {
            Logger.e(TAG, "Failed to send install event as Tracker is not instanced: %s", e.getMessage());
        }
    }

    private void sendInstallEvent(long installTimestamp) {
        SelfDescribing event = SelfDescribing.builder()
                .eventData(new SelfDescribingJson(TrackerConstants.SCHEMA_APPLICATION_INSTALL))
                .trueTimestamp(installTimestamp)
                .build();
        try {
            Tracker.instance().track(event);
        } catch(IllegalStateException e) {
            Logger.e(TAG, "Failed to send install event as Tracker is not instanced: %s", e.getMessage());
        }
    }

    @Nullable
    static public SelfDescribingJson getApplicationContext(@NonNull Context context) {
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            String versionName = pInfo.versionName;
            String versionCode = "";
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                versionCode = String.valueOf(pInfo.getLongVersionCode());
            } else {
                versionCode = String.valueOf(pInfo.versionCode);
            }
            if (versionName != null) {
                Map<String, Object> pairs = new HashMap<>();
                addToMap(Parameters.APP_VERSION, versionName, pairs);
                addToMap(Parameters.APP_BUILD, versionCode, pairs);
                return new SelfDescribingJson(
                        TrackerConstants.SCHEMA_APPLICATION, pairs
                );
            }
        } catch (PackageManager.NameNotFoundException e) {
            Logger.e(TAG, "Failed to find application context: %s", e.getMessage());
        }
        return null;
    }
}
