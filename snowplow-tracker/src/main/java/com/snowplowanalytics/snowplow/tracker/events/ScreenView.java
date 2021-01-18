/*
 * Copyright (c) 2015-2020 Snowplow Analytics Ltd. All rights reserved.
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

package com.snowplowanalytics.snowplow.tracker.events;

import android.app.Activity;
import android.app.Fragment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snowplowanalytics.snowplow.tracker.constants.Parameters;
import com.snowplowanalytics.snowplow.tracker.constants.TrackerConstants;
import com.snowplowanalytics.snowplow.tracker.tracker.ScreenState;
import com.snowplowanalytics.snowplow.tracker.utils.Logger;
import com.snowplowanalytics.snowplow.tracker.utils.Preconditions;
import com.snowplowanalytics.snowplow.tracker.payload.TrackerPayload;
import com.snowplowanalytics.snowplow.tracker.utils.Util;

import java.lang.reflect.Field;
import java.util.Map;

public class ScreenView extends AbstractSelfDescribing {

    private final static String TAG = ScreenView.class.getSimpleName();

    private final String name;
    private final String id;
    private String type;
    private String transitionType;
    private String previousName;
    private String previousId;
    private String previousType;
    private String fragmentClassName;
    private String fragmentTag;
    private String activityClassName;
    private String activityTag;

    public static abstract class Builder<T extends Builder<T>> extends AbstractEvent.Builder<T> {

        private String name;
        private String id;
        private String type;
        private String transitionType;
        private String previousName;
        private String previousId;
        private String previousType;
        private String fragmentClassName;
        private String fragmentTag;
        private String activityClassName;
        private String activityTag;

        /**
         * @param name The name of the screen view event
         * @return itself
         */
        @NonNull
        public T name(@NonNull String name) {
            this.name = name;
            return self();
        }

        /**
         * @param id Screen view ID
         * @return itself
         */
        @NonNull
        public T id(@NonNull String id) {
            this.id = id;
            return self();
        }

        /**
         * @param type The type of the screen view event
         * @return itself
         */
        @NonNull
        public T type(@NonNull String type) {
            this.type = type;
            return self();
        }

        /**
         * @param name The name from the previous screen view event
         * @return itself
         */
        @NonNull
        public T previousName(@NonNull String name) {
            this.previousName = name;
            return self();
        }

        /**
         * @param type The type from the previous screen view event
         * @return itself
         */
        @NonNull
        public T previousType(@NonNull String type) {
            this.previousType = type;
            return self();
        }

        /**
         * @param id The id from the previous screen view event
         * @return itself
         */
        @NonNull
        public T previousId(@Nullable String id) {
            this.previousId = id;
            return self();
        }

        /**
         * @param transitionType The transition type of the screen view event
         * @return itself
         */
        @NonNull
        public T transitionType(@Nullable String transitionType) {
            this.transitionType = transitionType;
            return self();
        }

        @NonNull
        public T fragmentClassName(@Nullable String fragmentClassName) {
            this.fragmentClassName = fragmentClassName;
            return self();
        }

        @NonNull
        public T fragmentTag(@Nullable String fragmentTag) {
            this.fragmentTag = fragmentTag;
            return self();
        }

        @NonNull
        public T activityClassName(@Nullable String activityClassName) {
            this.activityClassName = activityClassName;
            return self();
        }

        @NonNull
        public T activityTag(@Nullable String activityTag) {
            this.activityTag = activityTag;
            return self();
        }

        @NonNull
        public ScreenView build() {
            return new ScreenView(this);
        }
    }

    private static class Builder2 extends Builder<Builder2> {
        @Override
        protected Builder2 self() {
            return this;
        }
    }

    public static Builder<?> builder() {
        return new Builder2();
    }

    @NonNull
    public static ScreenView buildWithActivity(@NonNull Activity activity) {
        String activityClassName = activity.getLocalClassName();
        String activityTag = getSnowplowScreenId(activity);
        String name = getValidName(activityClassName, activityTag);
        return ScreenView.builder()
                .activityClassName(activityClassName)
                .activityTag(activityTag)
                .fragmentClassName(null)
                .fragmentTag(null)
                .name(name)
                .type(activityClassName)
                .transitionType(null)
                .build();
    }

    @NonNull
    public static ScreenView buildWithFragment(@NonNull Fragment fragment) {
        String fragmentClassName = fragment.getClass().getSimpleName();
        String fragmentTag = fragment.getTag();
        String name = getValidName(fragmentClassName, fragmentTag);
        return ScreenView.builder()
                .activityClassName(null)
                .activityTag(null)
                .fragmentClassName(fragment.getClass().getSimpleName())
                .fragmentTag(fragment.getTag())
                .name(name)
                .type(fragmentClassName)
                .transitionType(null)
                .build();
    }

    protected ScreenView(@NonNull Builder<?> builder) {
        super(builder);

        if (builder.id != null) {
            Preconditions.checkArgument(Util.isUUIDString(builder.id));
            id = builder.id;
        } else {
            id = Util.getUUIDString();
        }

        this.name = builder.name;
        this.type = builder.type;
        this.previousId = builder.previousId;
        this.previousName = builder.previousName;
        this.previousType = builder.previousType;
        this.transitionType = builder.transitionType;
        this.fragmentClassName = builder.fragmentClassName;
        this.fragmentTag = builder.fragmentTag;
        this.activityClassName = builder.activityClassName;
        this.activityTag = builder.activityTag;
    }

    /**
     * Update the passed screen state with the data related to
     * the current ScreenView event.
     * @apiNote ScreenState updates back the previous screen fields
     * in the ScreenView event (if `previousId` not already set).
     * @param screenState The screen state to update.
     */
    public synchronized void updateScreenState(@NonNull ScreenState screenState) {
        screenState.updateScreenState(id, name, type, transitionType, fragmentClassName, fragmentTag, activityClassName, activityTag);
        if (previousId == null) {
            previousId = screenState.getPreviousId();
            previousName = screenState.getPreviousName();
            previousType = screenState.getPreviousType();
        }
    }

    /**
     * Returns a TrackerPayload which can be stored into
     * the local database.
     *
     * @deprecated As of release 1.5.0, it will be removed in version 2.0.0.
     * replaced by {@link #getDataPayload()}.
     *
     * @return the payload to be sent.
     */
    @Deprecated
    @NonNull
    public TrackerPayload getData() {
        TrackerPayload payload = new TrackerPayload();
        payload.add(Parameters.SV_NAME, this.name);
        payload.add(Parameters.SV_ID, this.id);
        payload.add(Parameters.SV_TYPE, this.type);
        payload.add(Parameters.SV_PREVIOUS_ID, this.previousId);
        payload.add(Parameters.SV_PREVIOUS_NAME, this.previousName);
        payload.add(Parameters.SV_PREVIOUS_TYPE, this.previousType);
        payload.add(Parameters.SV_TRANSITION_TYPE, this.transitionType);
        return payload;
    }

    @Override
    public @NonNull Map<String, Object> getDataPayload() {
        return getData().getMap();
    }

    @Override
    public @NonNull String getSchema() {
        return TrackerConstants.SCHEMA_SCREEN_VIEW;
    }

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
