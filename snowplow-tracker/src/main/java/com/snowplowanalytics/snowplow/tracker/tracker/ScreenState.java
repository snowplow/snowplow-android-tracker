package com.snowplowanalytics.snowplow.tracker.tracker;

import com.snowplowanalytics.snowplow.tracker.constants.Parameters;
import com.snowplowanalytics.snowplow.tracker.constants.TrackerConstants;
import com.snowplowanalytics.snowplow.tracker.payload.SelfDescribingJson;
import com.snowplowanalytics.snowplow.tracker.payload.TrackerPayload;
import com.snowplowanalytics.snowplow.tracker.utils.Util;


public class ScreenState {
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

    @Deprecated
    public void newScreenState(String name, String type, String transitionType) {
        updateScreenState(Util.getUUIDString(), name, type, transitionType);
    }

    public synchronized void updateScreenState(String id, String name, String type, String transitionType) {
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

    public synchronized void updateScreenState(String id, String name, String type, String transitionType, String fragmentClassName, String fragmentTag, String activityClassName, String activityTag) {
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

    public SelfDescribingJson getCurrentScreen(Boolean debug) {
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

    @Deprecated
    public SelfDescribingJson getPreviousScreen(Boolean debug) {
        // not sure when this is useful (but make sure fragment/activity
        // isn't updated to current screen before calling this method)
        TrackerPayload contextPayload = new TrackerPayload();
        contextPayload.add(Parameters.SCREEN_ID, this.previousId);
        contextPayload.add(Parameters.SCREEN_NAME, this.previousName);
        contextPayload.add(Parameters.SCREEN_TYPE, this.previousType);
        return new SelfDescribingJson(
                TrackerConstants.SCHEMA_SCREEN,
                contextPayload);
    }

    @Deprecated
    public SelfDescribingJson getScreenViewEventJson() {
        TrackerPayload payload = new TrackerPayload();
        payload.add(Parameters.SV_NAME, this.name);
        payload.add(Parameters.SV_ID, this.id);
        payload.add(Parameters.SV_TYPE, this.type);
        payload.add(Parameters.SV_PREVIOUS_ID, this.previousId);
        payload.add(Parameters.SV_PREVIOUS_NAME, this.previousName);
        payload.add(Parameters.SV_PREVIOUS_TYPE, this.previousType);
        payload.add(Parameters.SV_TRANSITION_TYPE, this.transitionType);
        return new SelfDescribingJson(TrackerConstants.SCHEMA_SCREEN_VIEW, payload);
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
