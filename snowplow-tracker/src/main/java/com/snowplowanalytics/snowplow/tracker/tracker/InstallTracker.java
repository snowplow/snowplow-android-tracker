package com.snowplowanalytics.snowplow.tracker.tracker;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import com.snowplowanalytics.snowplow.tracker.Executor;
import com.snowplowanalytics.snowplow.tracker.Tracker;
import com.snowplowanalytics.snowplow.tracker.constants.Parameters;
import com.snowplowanalytics.snowplow.tracker.constants.TrackerConstants;
import com.snowplowanalytics.snowplow.tracker.events.SelfDescribing;
import com.snowplowanalytics.snowplow.tracker.payload.SelfDescribingJson;
import com.snowplowanalytics.snowplow.tracker.utils.FileStore;
import com.snowplowanalytics.snowplow.tracker.utils.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.snowplowanalytics.snowplow.tracker.utils.Util.addToMap;

public class InstallTracker {
    /**
     * Class used to keep track of install state of app.
     * If a file does not exist, the tracker will send an `application_install` event.
     */
    private static String TAG = InstallTracker.class.getSimpleName();
    private Boolean isNewInstall;
    private Context context;
    private Future infoFuture;

    public InstallTracker(Context context) {
        this.context = context;
        this.isNewInstall = false;
        this.infoFuture = null;
        checkInstall();
    }

    private void sendInstallEvent() {
        SelfDescribing event = SelfDescribing.builder()
                .eventData(new SelfDescribingJson(TrackerConstants.SCHEMA_APPLICATION_INSTALL))
                .build();
        Tracker.instance().track(event);
    }

    private void checkInstall() {
        this.infoFuture = Executor.futureCallable(new Callable<Map<String, Object>>() {
            @Override
            public Map<String,Object> call() {
                Map mapFromFile = FileStore.getMapFromFile(
                        TrackerConstants.SNOWPLOW_INSTALL_VARS,
                        context);
                isNewInstall = (mapFromFile == null);
                if (isNewInstall) {
                    sendInstallEvent();
                }
                Map<String, Object> installValues = getInstallValues();
                FileStore.saveMapToFile(
                        TrackerConstants.SNOWPLOW_INSTALL_VARS,
                        installValues,
                        context);
                return installValues;
            }
        });
    }

    private Map<String, Object> getInstallValues() {
        Map<String, Object> installValues = new HashMap<>();
        installValues.put(Parameters.INSTALL_STATUS, isNewInstall);
        return installValues;
    }

    private boolean waitForLoad() {
        if (infoFuture == null) {
            return false;
        }
        try {
            infoFuture.get(5, TimeUnit.SECONDS);
        } catch (InterruptedException ie) {
            Logger.e(TAG, "Install file loading was interrupted: %s", ie.getMessage());
        } catch (ExecutionException ee) {
            Logger.e(TAG, "Install file loading failed: %s", ee.getMessage());
        } catch (TimeoutException te) {
            Logger.e(TAG, "Install file loading timed out: %s", te.getMessage());
        }
        return infoFuture.isDone();
    }

    static public SelfDescribingJson getApplicationContext(Context context) {
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
