package com.snowplowanalytics.snowplow.tracker.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import rx.Scheduler;
import rx.Subscription;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

import com.snowplowanalytics.snowplow.tracker.Tracker;
import com.snowplowanalytics.snowplow.tracker.Emitter;
import com.snowplowanalytics.snowplow.tracker.storage.EventStore;
import com.snowplowanalytics.snowplow.tracker.utils.storage.EmittableEvents;
import com.snowplowanalytics.snowplow.tracker.utils.Logger;
import com.snowplowanalytics.snowplow.tracker.utils.emitter.RequestCallback;
import com.snowplowanalytics.snowplow.tracker.utils.emitter.RequestResult;

/**
* Service that emits any pending events.
*/
public class EmitterService extends Service {

    private static final String TAG = EmitterService.class.getSimpleName();

    private EventStore eventStore;
    private PublishSubject<EmittableEvents> eventSubject;
    private Subscription processingSubcription;
    private Scheduler scheduler;

    @Override
    public void onCreate() {

        super.onCreate();

        Logger.ifDebug(TAG, "onCreate()");

        final Emitter emitter = Tracker.getInstance().getEmitter();

        eventStore = Tracker.getInstance().getEventStore();

        scheduler = Schedulers.io();
        eventSubject = PublishSubject.create();
        eventSubject
            .subscribeOn(scheduler)
            .observeOn(scheduler)
            .unsubscribeOn(scheduler)
            .flatMap(emitter::emitEvent) // Sends events to the emitter...
            .doOnSubscribe(() -> Logger.ifDebug(TAG, "Got a subscriber"))
            .doOnUnsubscribe(() -> Logger.ifDebug(TAG, "Lost a subscriber"))
            .subscribe(results -> {

                Logger.ifDebug(TAG, "Got results: %s", results);

                // Get the emitter request callback
                RequestCallback callback = emitter.getRequestCallback();

                int successCount = 0;
                int failureCount = 0;

                // Loop through all request results
                for (RequestResult res : results) {

                    // If sending was a success
                    if (res.getSuccess()) {

                        successCount++;

                        Logger.ifDebug(TAG, "Request sending was a success!");

                        // Remove all eventIds associated with the request
                        for (Long eventId : res.getEventIds()) {
                            eventStore.remove(eventId);
                        }

                        Logger.ifDebug(TAG, "Event Store after cleanup: " + eventStore.size());
                    }
                    else if (!res.getSuccess()) {

                        failureCount++;

                        Logger.ifDebug(TAG, "Request sending failed but we can retry later!");

                        // Reset pending status on all events so they can be sent again...
                        for (Long eventId : res.getEventIds()) {
                            eventStore.removePending(eventId);
                        }

                        Logger.ifDebug(TAG, "Event Store after reset: " + eventStore.size());
                    }
                }

                // Send the callback if it is not null...
                if (callback != null) {
                    if (failureCount != 0) {
                        callback.onFailure(successCount, failureCount);
                    }
                    else {
                        callback.onSuccess(successCount);
                    }
                }

                // Shut the service down after it is done sending
                Logger.ifDebug(TAG, "Service going down...");
                shutdownService();
            });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Logger.ifDebug(TAG, "onStartCommand()");

        if (processingSubcription == null) {
            processingSubcription = eventStore.getEvents()
                .subscribeOn(scheduler)
                .unsubscribeOn(scheduler)
                .subscribe(event -> {
                    Logger.ifDebug(TAG, "events() emitted: %s", event);
                    eventSubject.onNext(event);
                });
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Logger.ifDebug(TAG, "onDestroy()");

        if (processingSubcription != null) {
            processingSubcription.unsubscribe();
            processingSubcription = null;
        }

        eventStore = null;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void shutdownService() {
        Logger.ifDebug(TAG, "shutdownService()");
        stopSelf();
    }
}
