package com.snowplowanalytics.snowplow.internal.utils;

import androidx.annotation.NonNull;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public class NotificationCenter {

    public abstract static class FunctionalObserver {
        public abstract void apply(@NonNull Map<String, Object> data);
    }

    private static class WeakObserver extends WeakReference<FunctionalObserver> {

        private boolean valid = true;

        public WeakObserver(FunctionalObserver referent) {
            super(referent);
        }

        public synchronized boolean isValid() {
            return valid && get() != null;
        }

        public synchronized void invalidate() {
            valid = false;
            clear();
        }
    }

    @NonNull
    private final static Map<String, List<WeakObserver>> notificationMap = new HashMap<>();
    private final static WeakHashMap<FunctionalObserver, WeakObserver> observerMap = new WeakHashMap<>();

    public synchronized static void addObserver(@NonNull String notificationType, @NonNull FunctionalObserver observer) {
        WeakObserver weakObserver = new WeakObserver(observer);
        WeakObserver previousObserver = observerMap.put(observer, weakObserver);
        if (previousObserver != null) {
            previousObserver.invalidate();
        }
        List<WeakObserver> observers = notificationMap.get(notificationType);
        if (observers == null) {
            observers = new LinkedList<>();
            notificationMap.put(notificationType, observers);
        }
        observers.add(weakObserver);
    }

    public synchronized static boolean removeObserver(@NonNull FunctionalObserver observer) {
        WeakObserver weakObserver = observerMap.remove(observer);
        if (weakObserver != null) {
            weakObserver.invalidate();
            return true;
        }
        return false;
    }

    public synchronized static void removeAll() {
        observerMap.clear();
        notificationMap.clear();
    }

    public synchronized static boolean postNotification(@NonNull String notificationType, @NonNull Map<String,Object> data) {
        List<WeakObserver> observers = notificationMap.get(notificationType);
        if (observers == null || observers.isEmpty()) {
            return false;
        }
        Iterator<WeakObserver> iterator = observers.iterator();
        while (iterator.hasNext()) {
            WeakObserver weakObserver = iterator.next();
            if (!weakObserver.isValid()) {
                synchronized (NotificationCenter.class) {
                    iterator.remove();
                }
                continue;
            }
            Map<String,Object> dataCopy = new HashMap<>(data);
            weakObserver.get().apply(dataCopy);
        }
        return !observers.isEmpty();
    }
}
