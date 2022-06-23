package com.snowplowanalytics.snowplow.tracker;

import android.test.AndroidTestCase;

import com.snowplowanalytics.snowplow.network.CollectorCookieJar;

import java.util.ArrayList;
import java.util.List;

import okhttp3.Cookie;
import okhttp3.HttpUrl;

public class CollectorCookieJarTest extends AndroidTestCase {
    Cookie cookie1 = new Cookie.Builder()
            .name("sp")
            .value("xxx")
            .domain("acme.test.url.com")
            .build();

    public void testNoCookiesAtStartup() {
        CollectorCookieJar cookieJar = new CollectorCookieJar(getContext());

        List<Cookie> cookies1 = cookieJar.loadForRequest(HttpUrl.parse("http://acme.test.url.com"));
        assertTrue(cookies1.isEmpty());
    }

    public void testReturnsCookiesAfterSetInResponse() {
        CollectorCookieJar cookieJar = new CollectorCookieJar(getContext());

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

    public void testDoesntReturnCookiesForDifferentDomain() {
        CollectorCookieJar cookieJar = new CollectorCookieJar(getContext());

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

    public void testMaintainsCookiesAcrossJarInstances() {
        CollectorCookieJar cookieJar1 = new CollectorCookieJar(getContext());

        ArrayList<Cookie> requestCookies = new ArrayList<Cookie>();
        requestCookies.add(cookie1);
        cookieJar1.saveFromResponse(
                HttpUrl.parse("http://acme.test.url.com"),
                requestCookies
        );

        CollectorCookieJar cookieJar2 = new CollectorCookieJar(getContext());

        List<Cookie> cookies2 = cookieJar2.loadForRequest(HttpUrl.parse("http://acme.test.url.com"));
        assertFalse(cookies2.isEmpty());

        cookieJar1.clear();
    }
}
