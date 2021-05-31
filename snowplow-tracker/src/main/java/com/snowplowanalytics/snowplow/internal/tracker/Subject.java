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

package com.snowplowanalytics.snowplow.internal.tracker;

import android.content.Context;
import android.graphics.Point;
import android.view.Display;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import com.snowplowanalytics.snowplow.configuration.SubjectConfiguration;
import com.snowplowanalytics.snowplow.internal.constants.Parameters;
import com.snowplowanalytics.snowplow.util.Size;

/**
 * Provides Subject information for each
 * event sent from the Snowplow Tracker.
 * @deprecated It will be removed in the next major version, please use Snowplow.setup methods.
 */
@Deprecated
public class Subject {

    private static final String TAG = Subject.class.getSimpleName();
    private final HashMap<String, String> standardPairs = new HashMap<>();

    @Nullable
    String userId;
    @Nullable
    String networkUserId;
    @Nullable
    String domainUserId;
    @Nullable
    String useragent;
    @Nullable
    String ipAddress;
    @Nullable
    String timezone;
    @Nullable
    String language;
    @Nullable
    Size screenResolution;
    @Nullable
    Size screenViewPort;
    @Nullable
    Integer colorDepth;


    /**
     * Builder for the Subject
     * @deprecated It will be removed in the next major version, please use Snowplow.setup methods.
     */
    @Deprecated
    public static class SubjectBuilder {
        private Context context = null; // Optional
        private SubjectConfigurationInterface subjectConfiguration = null; // Optional

        /**
         * @param context The android context to pass to the subject
         * @return itself
         */
        @NonNull
        public SubjectBuilder context(@NonNull Context context) {
            this.context = context;
            return this;
        }

        /**
         * @param subjectConfiguration The subjectConfiguration to configure the subject
         * @return itself
         */
        @NonNull
        public SubjectBuilder subjectConfiguration(@Nullable SubjectConfigurationInterface subjectConfiguration) {
            this.subjectConfiguration = subjectConfiguration;
            return this;
        }

        /**
         * Creates a new Subject
         *
         * @return a new Subject object
         */
        @NonNull
        public Subject build() {
            return new Subject(this);
        }
    }

    /**
     * Creates a Subject which will add extra data to each event.
     *
     * @param builder The builder that constructs a subject
     */
    private Subject(@NonNull SubjectBuilder builder) {
        setDefaultTimezone();
        setDefaultLanguage();
        if (builder.context != null) {
            setDefaultScreenResolution(builder.context);
        }
        SubjectConfigurationInterface config = builder.subjectConfiguration;
        if (config != null) {
            if (config.getUserId() != null) setUserId(config.getUserId());
            if (config.getNetworkUserId() != null) setNetworkUserId(config.getNetworkUserId());
            if (config.getDomainUserId() != null) setDomainUserId(config.getDomainUserId());
            if (config.getUseragent() != null) setUseragent(config.getUseragent());
            if (config.getIpAddress() != null) setIpAddress(config.getIpAddress());
            if (config.getTimezone() != null) setTimezone(config.getTimezone());
            if (config.getLanguage() != null) setLanguage(config.getLanguage());
            if (config.getScreenResolution() != null) {
                Size size = config.getScreenResolution();
                setScreenResolution(size.getWidth(), size.getHeight());
            }
            if (config.getScreenViewPort() != null) {
                Size size = config.getScreenViewPort();
                setViewPort(size.getWidth(), size.getHeight());
            }
            if (config.getColorDepth() != null) setColorDepth(config.getColorDepth());
        }
        Logger.v(TAG, "Subject created successfully.");
    }

    // Default information setters

    /**
     * Sets the default timezone of the
     * device.
     */
    private void setDefaultTimezone() {
        TimeZone tz = Calendar.getInstance().getTimeZone();
        this.setTimezone(tz.getID());
    }

    /**
     * Sets the default language of the
     * device.
     */
    private void setDefaultLanguage() {
        this.setLanguage(Locale.getDefault().getDisplayLanguage());
    }

    /**
     * Sets the default screen resolution
     * of the device the Tracker is running
     * on.
     *
     * @param context the android context
     */
    public void setDefaultScreenResolution(@NonNull Context context) {
        WindowManager windowManager =
                (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        Point size = new Point();

        display.getSize(size);
        this.setScreenResolution(size.x, size.y);
    }

    // Public Subject information setters

    /**
     * Sets the subjects userId
     * @deprecated Use {@link SubjectConfiguration#userId(String)}
     * @param userId a user id string
     */
    public void setUserId(@NonNull String userId) {
        this.userId = userId;
        this.standardPairs.put(Parameters.UID, userId);
    }

    /**
     * Sets the subjects userId
     * @deprecated Use {@link SubjectConfiguration#userId(String)}
     * @param userId a user id string
     */
    public void identifyUser(@NonNull String userId) { this.setUserId(userId); }

    /**
     * Sets a custom screen resolution based
     * on user inputted width and height.
     *
     * Measured in pixels: 1920x1080
     * @deprecated Use {@link SubjectConfiguration#screenResolution(Size)} 
     * @param width the width of the screen
     * @param height the height of the screen
     */
    public void setScreenResolution(int width, int height) {
        this.screenResolution = new Size(width, height);
        String res = Integer.toString(width) + "x" + Integer.toString(height);
        this.standardPairs.put(Parameters.RESOLUTION, res);
    }

    /**
     * Sets the view port resolution
     *
     * Measured in pixels: 1280x1024
     * @deprecated Use {@link SubjectConfiguration#screenViewPort(Size)} 
     * @param width the width of the viewport
     * @param height the height of the viewport
     */
    public void setViewPort(int width, int height) {
        this.screenViewPort = new Size(width, height);
        String res = Integer.toString(width) + "x" + Integer.toString(height);
        this.standardPairs.put(Parameters.VIEWPORT, res);
    }

    /**
     * user defined color depth.
     *
     * Measure as an integer
     * @deprecated Use {@link SubjectConfiguration#colorDepth(Integer)} 
     * @param depth the color depth
     */
    public void setColorDepth(int depth) {
        this.colorDepth = depth;
        this.standardPairs.put(Parameters.COLOR_DEPTH, Integer.toString(depth));
    }

    /**
     * User inputted timezone
     * @deprecated Use {@link SubjectConfiguration#timezone(String)} 
     * @param timezone a valid timezone
     */
    public void setTimezone(@NonNull String timezone) {
        this.timezone = timezone;
        this.standardPairs.put(Parameters.TIMEZONE, timezone);
    }

    /**
     * User inputted language for the
     * subject.
     * @deprecated Use {@link SubjectConfiguration#language(String)} 
     * @param language language setting
     */
    public void setLanguage(@NonNull String language) {
        this.language = language;
        this.standardPairs.put(Parameters.LANGUAGE, language);
    }

    /**
     * User inputted ip address for the
     * subject.
     * @deprecated Use {@link SubjectConfiguration#ipAddress(String)} 
     * @param ipAddress an ip address
     */
    public void setIpAddress(@NonNull String ipAddress) {
        this.ipAddress = ipAddress;
        this.standardPairs.put(Parameters.IP_ADDRESS, ipAddress);
    }

    /**
     * User inputted useragent for the
     * subject.
     * @deprecated Use {@link SubjectConfiguration#useragent(String)} 
     * @param useragent a useragent
     */
    public void setUseragent(@NonNull String useragent) {
        this.useragent = useragent;
        this.standardPairs.put(Parameters.USERAGENT, useragent);
    }

    /**
     * User inputted Network User Id for the
     * subject.
     * @deprecated Use {@link SubjectConfiguration#networkUserId(String)} 
     * @param networkUserId a network user id
     */
    public void setNetworkUserId(@NonNull String networkUserId) {
        this.networkUserId = networkUserId;
        this.standardPairs.put(Parameters.NETWORK_UID, networkUserId);
    }

    /**
     * User inputted Domain User Id for the
     * subject.
     * @deprecated Use {@link SubjectConfiguration#domainUserId(String)}
     * @param domainUserId a domain user id
     */
    public void setDomainUserId(@NonNull String domainUserId) {
        this.domainUserId = domainUserId;
        this.standardPairs.put(Parameters.DOMAIN_UID, domainUserId);
    }

    /**
     * @return the standard subject pairs
     */
    @NonNull
    public Map<String, String> getSubject() {
        return this.standardPairs;
    }
}
