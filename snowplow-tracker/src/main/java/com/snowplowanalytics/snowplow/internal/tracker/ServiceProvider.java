package com.snowplowanalytics.snowplow.internal.tracker;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.snowplowanalytics.snowplow.configuration.Configuration;
import com.snowplowanalytics.snowplow.configuration.NetworkConfiguration;
import com.snowplowanalytics.snowplow.configuration.TrackerConfiguration;
import com.snowplowanalytics.snowplow.tracker.Emitter;
import com.snowplowanalytics.snowplow.tracker.Subject;
import com.snowplowanalytics.snowplow.tracker.Tracker;
import com.snowplowanalytics.snowplow.tracker.emitter.HttpMethod;
import com.snowplowanalytics.snowplow.tracker.emitter.Protocol;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ServiceProvider {

    @NonNull
    private final Context context;
    @NonNull
    private final NetworkConfiguration networkConfiguration;
    @NonNull
    private final TrackerConfiguration trackerConfiguration;

    @Nullable
    private Tracker tracker;
    @Nullable
    private Emitter emitter;
    @Nullable
    private Subject subject;

    // Constructors

    private ServiceProvider(@NonNull Context context, @NonNull NetworkConfiguration networkConfiguration, @NonNull TrackerConfiguration trackerConfiguration, @NonNull List<Configuration> configurations) {
        Objects.requireNonNull(context);
        Objects.requireNonNull(networkConfiguration);
        Objects.requireNonNull(trackerConfiguration);
        Objects.requireNonNull(configurations);
        this.context = context;
        this.networkConfiguration = networkConfiguration;
        this.trackerConfiguration = trackerConfiguration;
        for (Configuration configuration : configurations) {
        }
        
    }

    // Setup

    @NonNull
    public static Tracker setup(@NonNull Context context, @NonNull String endpoint, @NonNull Protocol protocol, @NonNull HttpMethod method, @NonNull String namespace, @NonNull String appId) {
        NetworkConfiguration networkConfiguration = new NetworkConfiguration(endpoint, protocol, method);
        TrackerConfiguration trackerConfiguration = new TrackerConfiguration(namespace, appId);
        return setup(context, networkConfiguration, trackerConfiguration);
    }

    @NonNull
    public static Tracker setup(@NonNull Context context, @NonNull NetworkConfiguration networkConfiguration, @NonNull TrackerConfiguration trackerConfiguration) {
        return setup(context, networkConfiguration, trackerConfiguration, new ArrayList<>());
    }

    @NonNull
    public static Tracker setup(@NonNull Context context, @NonNull NetworkConfiguration networkConfiguration, @NonNull TrackerConfiguration trackerConfiguration, @NonNull List<Configuration> configurations) {
        ServiceProvider serviceProvider = new ServiceProvider(context, networkConfiguration, trackerConfiguration, configurations);
        return serviceProvider.getTracker();
    }

    // Getters

    @NonNull
    private Tracker getTracker() {
        if (tracker == null) {
            tracker = makeTracker();
        }
        return tracker;
    }

    @NonNull
    private Emitter getEmitter() {
        if (emitter == null) {
            emitter = makeEmitter();
        }
        return emitter;
    }

    @NonNull
    private Subject getSubject() {
        if (subject == null) {
            subject = makeSubject();
        }
        return subject;
    }

    // Factories

    @NonNull
    private Subject makeSubject() {
        return new Subject.SubjectBuilder()
                .context(null)  // TODO: Add context
                .build(); // TODO: Add SubjectConfiguration parameter in constructor
    }

    @NonNull
    private Emitter makeEmitter() {
        return new Emitter.EmitterBuilder(networkConfiguration.getEndpoint(), context)  // TODO: Add context
                .networkConnection(networkConfiguration.networkConnection)
                .method(networkConfiguration.getMethod())
                .security(networkConfiguration.getProtocol())
                .customPostPath(networkConfiguration.customPostPath)
                // TODO: complete settings with EmitterConfiguration
                .build();
    }

    @NonNull
    private Tracker makeTracker() {
        Emitter emitter = getEmitter();
        Subject subject = getSubject();
        return new Tracker.TrackerBuilder(emitter, trackerConfiguration.namespace, trackerConfiguration.appId, context)
                .subject(subject)
                .base64(trackerConfiguration.base64encoding)
                .level(trackerConfiguration.logLevel)
                .loggerDelegate(trackerConfiguration.loggerDelegate)
                .platform(trackerConfiguration.devicePlatform)
                .sessionContext(trackerConfiguration.sessionContext)
                .applicationContext(trackerConfiguration.applicationContext)
                .screenContext(trackerConfiguration.screenContext)
                .screenviewEvents(trackerConfiguration.screenViewAutotracking)
                .lifecycleEvents(trackerConfiguration.lifecycleAutotracking)
                .installTracking(trackerConfiguration.installAutotracking)
                .applicationCrash(trackerConfiguration.exceptionAutotracking)
                .trackerDiagnostic(trackerConfiguration.diagnosticAutotracking)

                // TODO: complete settings with all the other configurations
                .build();
    }

}
