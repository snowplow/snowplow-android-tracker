package com.snowplowanalytics.snowplow.tracker

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.snowplowanalytics.core.constants.TrackerConstants
import com.snowplowanalytics.snowplow.network.CollectorCookieJar
import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CollectorCookieJarTest {
    private var cookie1: Cookie = Cookie.Builder()
        .name("sp")
        .value("xxx")
        .domain("acme.test.url.com")
        .build()

    @Test
    fun testNoCookiesAtStartup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val cookieJar = CollectorCookieJar(context)
        val cookies1 = cookieJar.loadForRequest("http://acme.test.url.com".toHttpUrlOrNull()!!)
        Assert.assertTrue(cookies1.isEmpty())
    }

    @Test
    fun testReturnsCookiesAfterSetInResponse() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val cookieJar = CollectorCookieJar(context)
        val requestCookies = ArrayList<Cookie>()
        requestCookies.add(cookie1)
        cookieJar.saveFromResponse(
            "http://acme.test.url.com".toHttpUrlOrNull()!!,
            requestCookies
        )
        val cookies2 = cookieJar.loadForRequest("http://acme.test.url.com".toHttpUrlOrNull()!!)
        Assert.assertFalse(cookies2.isEmpty())
        Assert.assertEquals(cookies2[0].name, "sp")
        cookieJar.clear()
    }

    @Test
    fun testDoesntReturnCookiesForDifferentDomain() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val cookieJar = CollectorCookieJar(context)
        val requestCookies = ArrayList<Cookie>()
        requestCookies.add(cookie1)
        cookieJar.saveFromResponse(
            "http://acme.test.url.com".toHttpUrlOrNull()!!,
            requestCookies
        )
        val cookies2 = cookieJar.loadForRequest("http://other.test.url.com".toHttpUrlOrNull()!!)
        Assert.assertTrue(cookies2.isEmpty())
        cookieJar.clear()
    }

    @Test
    fun testMaintainsCookiesAcrossJarInstances() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val cookieJar1 = CollectorCookieJar(context)
        val requestCookies = ArrayList<Cookie>()
        requestCookies.add(cookie1)
        cookieJar1.saveFromResponse(
            "http://acme.test.url.com".toHttpUrlOrNull()!!,
            requestCookies
        )
        val cookieJar2 = CollectorCookieJar(context)
        val cookies2 = cookieJar2.loadForRequest("http://acme.test.url.com".toHttpUrlOrNull()!!)
        Assert.assertFalse(cookies2.isEmpty())
        cookieJar1.clear()
    }

    @Test
    fun testRemovesInvalidCookies() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val sharedPreferences =
            context.getSharedPreferences(TrackerConstants.COOKIE_PERSISTANCE, Context.MODE_PRIVATE)
        sharedPreferences.edit().putString("x", "y").apply()
        Assert.assertEquals(1, sharedPreferences.all.size.toLong())
        CollectorCookieJar(context)
        Assert.assertEquals(0, sharedPreferences.all.size.toLong())
    }
}
