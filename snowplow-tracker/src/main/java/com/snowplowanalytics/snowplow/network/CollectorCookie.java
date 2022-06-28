package com.snowplowanalytics.snowplow.network;

import org.json.JSONException;
import org.json.JSONObject;

import okhttp3.Cookie;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class CollectorCookie {

    private final Cookie cookie;

    static List<CollectorCookie> decorateAll(Collection<Cookie> cookies) {
        List<CollectorCookie> collectorCookies = new ArrayList<>(cookies.size());
        for (Cookie cookie : cookies) {
            collectorCookies.add(new CollectorCookie(cookie));
        }
        return collectorCookies;
    }

    CollectorCookie(Cookie cookie) {
        this.cookie = cookie;
    }

    CollectorCookie(String serialized) throws JSONException {
        JSONObject object = new JSONObject(serialized);
        cookie = new Cookie.Builder()
                .name(object.getString("name"))
                .value(object.getString("value"))
                .expiresAt(object.getLong("expiresAt"))
                .domain(object.getString("domain"))
                .path(object.getString("path"))
                .build();
    }

    public boolean isExpired() {
        return cookie.expiresAt() < System.currentTimeMillis();
    }

    Cookie getCookie() {
        return cookie;
    }

    String getCookieKey() {
        return (cookie.secure() ? "https" : "http") + "://" + cookie.domain() + cookie.path() + "|" + cookie.name();
    }

    public String serialize() {
        HashMap<String, Object> values = new HashMap<String, Object>();
        values.put("name", cookie.name());
        values.put("value", cookie.value());
        values.put("expiresAt", cookie.expiresAt());
        values.put("domain", cookie.domain());
        values.put("path", cookie.path());
        return new JSONObject(values).toString();
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof CollectorCookie)) return false;
        CollectorCookie that = (CollectorCookie) other;
        return that.cookie.name().equals(this.cookie.name())
                && that.cookie.domain().equals(this.cookie.domain())
                && that.cookie.path().equals(this.cookie.path());
    }

    @Override
    public int hashCode() {
        int hash = 17;
        hash = 31 * hash + cookie.name().hashCode();
        hash = 31 * hash + cookie.domain().hashCode();
        hash = 31 * hash + cookie.path().hashCode();
        return hash;
    }
}
