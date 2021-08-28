package com.snowplowanalytics.snowplow.internal.tracker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import com.snowplowanalytics.snowplow.internal.constants.Parameters;
import com.snowplowanalytics.snowplow.internal.constants.TrackerConstants;
import com.snowplowanalytics.snowplow.payload.SelfDescribingJson;
import com.snowplowanalytics.snowplow.payload.TrackerPayload;
import com.snowplowanalytics.snowplow.internal.utils.Util;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class ScreenState implements State {
    private String name;
    private String type;
    private String id;
    private String previousName;
    private String previousId;
    private String previousType;
    private String transitionType;
    private String fragmentClassName;
    private String fragmentTag;
    private String activityClassName;
    private String activityTag;

    public ScreenState() {
        id = Util.getUUIDString();
        name = "Unknown";
    }

    @Nullable
    public String getPreviousId() {
        return previousId;
    }

    @Nullable
    public String getPreviousName() {
        return previousName;
    }

    @Nullable
    public String getPreviousType() {
        return previousType;
    }

    public synchronized void updateScreenState(@NonNull String id, @NonNull String name, @Nullable String type, @Nullable String transitionType) {
        this.populatePreviousFields();
        this.name = name;
        this.type = type;
        this.transitionType = transitionType;
        if (id != null) {
            this.id = id;
        } else {
            this.id = Util.getUUIDString();
        }
    }

    public synchronized void updateScreenState(@NonNull String id, @NonNull String name, @Nullable String type, @Nullable String transitionType, @Nullable String fragmentClassName, @Nullable String fragmentTag, @Nullable String activityClassName, @Nullable String activityTag) {
        this.updateScreenState(id, name, type, transitionType);
        this.fragmentClassName = fragmentClassName;
        this.fragmentTag = fragmentTag;
        this.activityClassName = activityClassName;
        this.activityTag = activityTag;
    }

    public void populatePreviousFields() {
        this.previousName = this.name;
        this.previousType = this.type;
        this.previousId = this.id;
    }

    @NonNull
    public SelfDescribingJson getCurrentScreen(boolean debug) {
        // this creates a screen context from screen state
        TrackerPayload contextPayload = new TrackerPayload();
        contextPayload.add(Parameters.SCREEN_ID, this.id);
        contextPayload.add(Parameters.SCREEN_NAME, this.name);
        contextPayload.add(Parameters.SCREEN_TYPE, this.type);
        if (debug) {
            contextPayload.add(Parameters.SCREEN_FRAGMENT, getValidName(fragmentClassName, fragmentTag));
            contextPayload.add(Parameters.SCREEN_ACTIVITY, getValidName(activityClassName, activityTag));
        }
        return new SelfDescribingJson(
                TrackerConstants.SCHEMA_SCREEN,
                contextPayload);
    }

    // Private methods

    private String getValidName(String s1, String s2) {
        if (s1 != null && s1.length() > 0) {
            return s1;
        }
        if (s2 != null && s2.length() > 0) {
            return s2;
        }
        return null;
    }
}
