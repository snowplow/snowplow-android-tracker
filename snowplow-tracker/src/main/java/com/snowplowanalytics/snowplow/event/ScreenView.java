/*
 * Copyright (c) 2015-2022 Snowplow Analytics Ltd. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */

package com.snowplowanalytics.snowplow.event;

import android.app.Activity;
import android.app.Fragment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snowplowanalytics.snowplow.internal.constants.Parameters;
import com.snowplowanalytics.snowplow.internal.constants.TrackerConstants;
import com.snowplowanalytics.snowplow.internal.tracker.Logger;
import com.snowplowanalytics.snowplow.internal.utils.Preconditions;
import com.snowplowanalytics.snowplow.internal.utils.Util;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** A ScreenView event. */
public class ScreenView extends AbstractSelfDescribing {

    private final static String TAG = ScreenView.class.getSimpleName();

    /** Name of the screen. */
    @NonNull
    public final String name;
    /** Identifier of the screen. */
    @NonNull
    public final String id;
    /** Type of screen. */
    @Nullable
    public String type;
    /** Name of the previous screen. */
    @Nullable
    public String previousName;
    /** Identifier of the previous screen. */
    @Nullable
    public String previousId;
    /** Type of the previous screen. */
    @Nullable
    public String previousType;
    /** Type of transition between previous and current screen. */
    @Nullable
    public String transitionType;
    /** Name of the Fragment subclass. */
    @Nullable
    public String fragmentClassName;
    /** Tag of the Fragment subclass. */
    @Nullable
    public String fragmentTag;
    /** Name of the Activity subclass. */
    @Nullable
    public String activityClassName;
    /** Tag of the Activity subclass. */
    @Nullable
    public String activityTag;

    /** Creates a ScreenView event using the data of an Activity class. */
    @NonNull
    public static ScreenView buildWithActivity(@NonNull Activity activity) {
        String activityClassName = activity.getLocalClassName();
        String activityTag = getSnowplowScreenId(activity);
        String name = getValidName(activityClassName, activityTag);
        return new ScreenView(name)
                .activityClassName(activityClassName)
                .activityTag(activityTag)
                .fragmentClassName(null)
                .fragmentTag(null)
                .type(activityClassName)
                .transitionType(null);
    }

    /** Creates a ScreenView event using the data of an Fragment class. */
    @NonNull
    public static ScreenView buildWithFragment(@NonNull Fragment fragment) {
        String fragmentClassName = fragment.getClass().getSimpleName();
        String fragmentTag = fragment.getTag();
        String name = getValidName(fragmentClassName, fragmentTag);
        return new ScreenView(name)
                .activityClassName(null)
                .activityTag(null)
                .fragmentClassName(fragment.getClass().getSimpleName())
                .fragmentTag(fragment.getTag())
                .type(fragmentClassName)
                .transitionType(null);
    }

    /**
     * Creates a ScreenView event.
     * @param name Name of the screen.
     */
    public ScreenView(@NonNull String name) {
        this(name, null);
    }

    /**
     * Creates a ScreenView event.
     * @param name Name of the screen.
     * @param screenId Identifier of the screen.
     */
    public ScreenView(@NonNull String name, @Nullable UUID screenId) {
        Preconditions.checkNotNull(name);
        Preconditions.checkArgument(!name.isEmpty(), "Name cannot be empty.");
        this.name = name;
        if (screenId != null) {
            id = screenId.toString();
        } else {
            id = Util.getUUIDString();
        }
    }

    // Builder methods

    /** Type of screen. */
    @NonNull
    public ScreenView type(@Nullable String type) {
        this.type = type;
        return this;
    }

    /** Name of the previous screen. */
    @NonNull
    public ScreenView previousName(@Nullable String previousName) {
        this.previousName = previousName;
        return this;
    }

    /** Identifier of the previous screen. */
    @NonNull
    public ScreenView previousId(@Nullable String previousId) {
        this.previousId = previousId;
        return this;
    }

    /** Type of the previous screen. */
    @NonNull
    public ScreenView previousType(@Nullable String previousType) {
        this.previousType = previousType;
        return this;
    }

    /** Type of transition between previous and current screen. */
    @NonNull
    public ScreenView transitionType(@Nullable String transitionType) {
        this.transitionType = transitionType;
        return this;
    }

    /** Name of the Fragment subclass. */
    @NonNull
    public ScreenView fragmentClassName(@Nullable String fragmentClassName) {
        this.fragmentClassName = fragmentClassName;
        return this;
    }

    /** Tag of the Fragment subclass. */
    @NonNull
    public ScreenView fragmentTag(@Nullable String fragmentTag) {
        this.fragmentTag = fragmentTag;
        return this;
    }

    /** Name of the Activity subclass. */
    @NonNull
    public ScreenView activityClassName(@Nullable String activityClassName) {
        this.activityClassName = activityClassName;
        return this;
    }

    /** Tag of the Activity subclass. */
    @NonNull
    public ScreenView activityTag(@Nullable String activityTag) {
        this.activityTag = activityTag;
        return this;
    }

    // Tracker methods

    @Override
    public @NonNull Map<String, Object> getDataPayload() {
        HashMap<String,Object> payload = new HashMap<>();
        payload.put(Parameters.SV_ID, id);
        payload.put(Parameters.SV_NAME, name);
        if (type != null) payload.put(Parameters.SV_TYPE, type);
        if (previousId != null) payload.put(Parameters.SV_PREVIOUS_ID, previousId);
        if (previousName != null) payload.put(Parameters.SV_PREVIOUS_NAME, previousName);
        if (previousType != null) payload.put(Parameters.SV_PREVIOUS_TYPE, previousType);
        if (transitionType != null) payload.put(Parameters.SV_TRANSITION_TYPE, transitionType);
        return payload;
    }

    @Override
    public @NonNull String getSchema() {
        return TrackerConstants.SCHEMA_SCREEN_VIEW;
    }

    // Private methods

    @Nullable
    private static String getSnowplowScreenId(Activity activity) {
        Class<? extends Activity> activityClass = activity.getClass();
        try {
            Field field = activityClass.getField("snowplowScreenId");
            Object reflectedValue = field.get(activity);
            if (reflectedValue instanceof String) {
                return (String) reflectedValue;
            } else {
                Logger.e(TAG,String.format("The value of field `snowplowScreenId` on Activity `%s` has to be a String.", activityClass.getSimpleName()));
            }
        } catch (NoSuchFieldException e) {
            Logger.d(TAG, String.format("Field `snowplowScreenId` not found on Activity `%s`.", activityClass.getSimpleName()), e);
        } catch (Exception e) {
            Logger.e(TAG, "Error retrieving value of field `snowplowScreenId`: " + e.getMessage(), e);
        }
        return null;
    }

    @NonNull
    private static String getValidName(@Nullable String s1, @Nullable String s2) {
        if (s1 != null && s1.length() > 0) {
            return s1;
        }
        if (s2 != null && s2.length() > 0) {
            return s2;
        }
        return "Unknown";
    }
}
