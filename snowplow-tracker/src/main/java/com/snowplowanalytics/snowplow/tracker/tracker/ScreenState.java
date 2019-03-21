package com.snowplowanalytics.snowplow.tracker.tracker;

import android.app.Activity;
import android.app.Fragment;

import com.snowplowanalytics.snowplow.tracker.constants.Parameters;
import com.snowplowanalytics.snowplow.tracker.constants.TrackerConstants;
import com.snowplowanalytics.snowplow.tracker.payload.SelfDescribingJson;
import com.snowplowanalytics.snowplow.tracker.payload.TrackerPayload;

import java.lang.reflect.Field;
import java.util.UUID;

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
        this.generateNewId();
    }

    public ScreenState(String name, String type, String transitionType) {
        this();
        this.name = name;
        this.type = type;
        this.transitionType = transitionType;
    }

    public ScreenState(String name, String type, String transitionType,
                       ScreenState previousState) {
        this(name, type, transitionType);
        this.previousId = previousState.id;
        this.previousName = previousState.name;
        this.previousType = previousState.type;
    }

    public void newScreenState(String name, String type, String transitionType) {
        this.populatePreviousFields();
        this.name = name;
        this.type = type;
        this.transitionType = transitionType;
        this.generateNewId();
    }

    public static String getSnowplowScreenId(Activity activity) {
        try {
            Field field = activity.getClass().getField("snowplowScreenId");
            Object reflectedValue = field.get(activity);
            if (reflectedValue instanceof String) {
                return (String)reflectedValue;
            }
        } catch (Exception e) {
            // log here
        }
        return null;
    }

    public void generateNewId() {
        this.id = UUID.randomUUID().toString();
    }

    public void updateWithActivity(Activity activity) {
        this.activityClassName = activity.getLocalClassName();
        this.activityTag = getSnowplowScreenId(activity);
        this.fragmentTag = null;
        this.fragmentClassName = null;
        this.generateNewId();
        // shift current fields to previous
        this.populatePreviousFields();
        // now fill current fields with new values
        this.name = getAutomaticName();
        this.type = getAutomaticType();
        this.transitionType = null;
    }

    public void updateWithFragment(Fragment fragment) {
        this.fragmentClassName = fragment.getClass().getSimpleName();
        this.fragmentTag = fragment.getTag();
        this.generateNewId();
        // shift current fields to previous
        this.populatePreviousFields();
        // now fill current fields with new values
        this.name = getAutomaticName();
        this.type = getAutomaticType();
        this.transitionType = null;
    }

    public void populatePreviousFields() {
        this.previousName = this.name;
        this.previousType = this.type;
        this.previousId = this.id;
    }

    public String getAutomaticName() {
        String activity = getActivityField();
        String fragment = getFragmentField();
        if (activity != null) {
            return activity;
        }
        if (fragment != null) {
            return fragment;
        }
        return "Unknown";
    }

    public String getAutomaticType() {
        if (this.fragmentClassName != null) {
            if (this.fragmentClassName.length() > 0) {
                return this.fragmentClassName;
            }
        }
        if (this.activityClassName != null) {
            if (this.activityClassName.length() > 0) {
                return this.activityClassName;
            }
        }
        return null;
    }

    public String getActivityField() {
        if (this.activityClassName != null) {
            if (this.activityClassName.length() > 0){
                return activityClassName;
            }
        }
        if (this.activityTag != null) {
            if (this.activityTag.length() > 0) {
                return activityTag;
            }
        }
        return null;
    }

    public String getFragmentField() {
        if (this.fragmentClassName != null) {
            if (this.fragmentClassName.length() > 0) {
                return fragmentClassName;
            }
        }
        if (this.fragmentTag != null) {
            if (this.fragmentTag.length() > 0) {
                return fragmentTag;
            }
        }
        return null;
    }

    public boolean isValid() {
        return (name != null) && (id != null);
    }

    public SelfDescribingJson getCurrentScreen(Boolean debug) {
        // this creates a screen context from screen state
        TrackerPayload contextPayload = new TrackerPayload();
        contextPayload.add(Parameters.SCREEN_ID, this.id);
        contextPayload.add(Parameters.SCREEN_NAME, this.name);
        contextPayload.add(Parameters.SCREEN_TYPE, this.type);
        if (debug) {
            contextPayload.add(Parameters.SCREEN_FRAGMENT, this.getFragmentField());
            contextPayload.add(Parameters.SCREEN_ACTIVITY, this.getActivityField());
        }
        return new SelfDescribingJson(
                TrackerConstants.SCHEMA_SCREEN,
                contextPayload);
    }

    public SelfDescribingJson getPreviousScreen(Boolean debug) {
        // not sure when this is useful (but make sure fragment/activity
        // isn't updated to current screen before calling this method)
        TrackerPayload contextPayload = new TrackerPayload();
        contextPayload.add(Parameters.SCREEN_ID, this.previousId);
        contextPayload.add(Parameters.SCREEN_NAME, this.previousName);
        contextPayload.add(Parameters.SCREEN_TYPE, this.previousType);
        if (debug) {
            contextPayload.add(Parameters.SCREEN_FRAGMENT, this.getFragmentField());
            contextPayload.add(Parameters.SCREEN_ACTIVITY, this.getActivityField());
        }
        return new SelfDescribingJson(
                TrackerConstants.SCHEMA_SCREEN,
                contextPayload);
    }

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
}
