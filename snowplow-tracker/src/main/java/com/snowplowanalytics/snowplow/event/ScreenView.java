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

package com.snowplowanalytics.snowplow.event;

import android.app.Activity;
import android.app.Fragment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snowplowanalytics.snowplow.internal.constants.Parameters;
import com.snowplowanalytics.snowplow.internal.constants.TrackerConstants;
import com.snowplowanalytics.snowplow.internal.tracker.ScreenState;
import com.snowplowanalytics.snowplow.internal.tracker.Logger;
import com.snowplowanalytics.snowplow.internal.utils.Preconditions;
import com.snowplowanalytics.snowplow.payload.TrackerPayload;
import com.snowplowanalytics.snowplow.internal.utils.Util;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ScreenView extends AbstractSelfDescribing {

    private final static String TAG = ScreenView.class.getSimpleName();

    @NonNull
    public final String name;
    @NonNull
    public final String id;
    @Nullable
    public String type;
    @Nullable
    public String transitionType;
    @Nullable
    public String previousName;
    @Nullable
    public String previousId;
    @Nullable
    public String previousType;
    @Nullable
    public String fragmentClassName;
    @Nullable
    public String fragmentTag;
    @Nullable
    public String activityClassName;
    @Nullable
    public String activityTag;

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
        @NonNull
        @Override
        protected Builder2 self() {
            return this;
        }
    }

    @NonNull
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
        Preconditions.checkNotNull(builder.name);
        Preconditions.checkArgument(!builder.name.isEmpty(), "Name cannot be empty.");
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

    @NonNull
    public ScreenView type(@Nullable String type) {
        this.type = type;
        return this;
    }

    @NonNull
    public ScreenView transitionType(@Nullable String transitionType) {
        this.transitionType = transitionType;
        return this;
    }

    @NonNull
    public ScreenView previousName(@Nullable String previousName) {
        this.previousName = previousName;
        return this;
    }

    @NonNull
    public ScreenView previousId(@Nullable String previousId) {
        this.previousId = previousId;
        return this;
    }

    @NonNull
    public ScreenView previousType(@Nullable String previousType) {
        this.previousType = previousType;
        return this;
    }

    @NonNull
    public ScreenView fragmentClassName(@Nullable String fragmentClassName) {
        this.fragmentClassName = fragmentClassName;
        return this;
    }

    @NonNull
    public ScreenView fragmentTag(@Nullable String fragmentTag) {
        this.fragmentTag = fragmentTag;
        return this;
    }

    @NonNull
    public ScreenView activityClassName(@Nullable String activityClassName) {
        this.activityClassName = activityClassName;
        return this;
    }

    @NonNull
    public ScreenView activityTag(@Nullable String activityTag) {
        this.activityTag = activityTag;
        return this;
    }

    // Public methods

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
