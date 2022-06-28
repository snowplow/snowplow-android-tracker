package com.snowplowanalytics.snowplow.tracker;

import static com.snowplowanalytics.snowplow.internal.constants.TrackerConstants.COOKIE_PERSISTANCE;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.snowplowanalytics.snowplow.network.CollectorCookieJar;

import java.util.ArrayList;
import java.util.List;

import okhttp3.Cookie;
import okhttp3.HttpUrl;

@RunWith(AndroidJUnit4.class)
public class CollectorCookieJarTest {
    Cookie cookie1 = new Cookie.Builder()
            .name("sp")
            .value("xxx")
            .domain("acme.test.url.com")
            .build();

    @Test
    public void testNoCookiesAtStartup() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        CollectorCookieJar cookieJar = new CollectorCookieJar(context);

        List<Cookie> cookies1 = cookieJar.loadForRequest(HttpUrl.parse("http://acme.test.url.com"));
        assertTrue(cookies1.isEmpty());
    }

    @Test
    public void testReturnsCookiesAfterSetInResponse() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        CollectorCookieJar cookieJar = new CollectorCookieJar(context);

        ArrayList<Cookie> requestCookies = new ArrayList<Cookie>();
        requestCookies.add(cookie1);
        cookieJar.saveFromResponse(
                HttpUrl.parse("http://acme.test.url.com"),
                requestCookies
        );

        List<Cookie> cookies2 = cookieJar.loadForRequest(HttpUrl.parse("http://acme.test.url.com"));
        assertFalse(cookies2.isEmpty());
        assertEquals(cookies2.get(0).name(), "sp");

        cookieJar.clear();
    }

    @Test
    public void testDoesntReturnCookiesForDifferentDomain() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        CollectorCookieJar cookieJar = new CollectorCookieJar(context);

        ArrayList<Cookie> requestCookies = new ArrayList<Cookie>();
        requestCookies.add(cookie1);
        cookieJar.saveFromResponse(
                HttpUrl.parse("http://acme.test.url.com"),
                requestCookies
        );

        List<Cookie> cookies2 = cookieJar.loadForRequest(HttpUrl.parse("http://other.test.url.com"));
        assertTrue(cookies2.isEmpty());

        cookieJar.clear();
    }

    @Test
    public void testMaintainsCookiesAcrossJarInstances() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        CollectorCookieJar cookieJar1 = new CollectorCookieJar(context);

        ArrayList<Cookie> requestCookies = new ArrayList<Cookie>();
        requestCookies.add(cookie1);
        cookieJar1.saveFromResponse(
                HttpUrl.parse("http://acme.test.url.com"),
                requestCookies
        );

        CollectorCookieJar cookieJar2 = new CollectorCookieJar(context);

        List<Cookie> cookies2 = cookieJar2.loadForRequest(HttpUrl.parse("http://acme.test.url.com"));
        assertFalse(cookies2.isEmpty());

        cookieJar1.clear();
    }

    @Test
    public void testRemovesInvalidCookies() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        SharedPreferences sharedPreferences = context.getSharedPreferences(COOKIE_PERSISTANCE, Context.MODE_PRIVATE);
        sharedPreferences.edit().putString("x", "y").apply();
        assertEquals(1, sharedPreferences.getAll().size());

        new CollectorCookieJar(context);
        assertEquals(0, sharedPreferences.getAll().size());
    }
}
