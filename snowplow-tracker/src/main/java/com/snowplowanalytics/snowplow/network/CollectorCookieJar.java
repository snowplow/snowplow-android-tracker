package com.snowplowanalytics.snowplow.network;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import okhttp3.Cookie;
import okhttp3.HttpUrl;

import static com.snowplowanalytics.snowplow.internal.constants.TrackerConstants.COOKIE_PERSISTANCE;

public class CollectorCookieJar implements okhttp3.CookieJar {
    private Set<CollectorCookie> cookies;
    private final SharedPreferences sharedPreferences;

    public CollectorCookieJar(@NonNull Context context) {
        cookies = Collections.newSetFromMap(new ConcurrentHashMap<CollectorCookie, Boolean>());
        sharedPreferences = context.getSharedPreferences(COOKIE_PERSISTANCE, Context.MODE_PRIVATE);

        loadFromSharedPreferences();
    }

    @NonNull
    @Override
    public List<Cookie> loadForRequest(@NonNull HttpUrl url) {
        List<CollectorCookie> cookiesToRemove = new ArrayList<>();
        List<Cookie> validCookies = new ArrayList<>();

        for (CollectorCookie currentCookie : cookies) {
            if (currentCookie.isExpired()) {
                cookiesToRemove.add(currentCookie);
            } else if (currentCookie.getCookie().matches(url)) {
                validCookies.add(currentCookie.getCookie());
            }
        }

        if (!cookiesToRemove.isEmpty()) {
            removeAll(cookiesToRemove);
        }

        return validCookies;
    }

    @Override
    public void saveFromResponse(@NonNull HttpUrl httpUrl, @NonNull List<Cookie> cookies) {
        saveAll(cookies);
    }

    public void clear() {
        sharedPreferences.edit().clear().apply();
        cookies.clear();
    }

    private void loadFromSharedPreferences() {
        List<String> cookiesToRemove = new ArrayList<>();
        for (Map.Entry<String, ?> entry : sharedPreferences.getAll().entrySet()) {
            String serializedCookie = (String) entry.getValue();

            if (serializedCookie == null) {
                continue;
            }

            try {
                CollectorCookie cookie = new CollectorCookie(serializedCookie);
                cookies.add(cookie);
            } catch (JSONException ignored) {
                cookiesToRemove.add(entry.getKey());
            }
        }

        if (!cookiesToRemove.isEmpty()) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            for (String cookie : cookiesToRemove) {
                editor.remove(cookie);
            }
            editor.apply();
        }
    }

    private void saveAll(Collection<Cookie> newCookies) {
        SharedPreferences.Editor editor = sharedPreferences.edit();

        for (CollectorCookie cookie : CollectorCookie.decorateAll(newCookies)) {
            cookies.remove(cookie);
            cookies.add(cookie);

            editor.putString(cookie.getCookieKey(), cookie.serialize());
        }

        editor.apply();
    }

    private void removeAll(Collection<CollectorCookie> cookiesToRemove) {
        SharedPreferences.Editor editor = sharedPreferences.edit();

        for (CollectorCookie cookie : cookiesToRemove) {
            cookies.remove(cookie);
            editor.remove(cookie.getCookieKey());
        }

        editor.apply();
    }
}
