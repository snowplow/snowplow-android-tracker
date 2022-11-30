package com.snowplowanalytics.snowplow.network

import okhttp3.Cookie
import okhttp3.Cookie.Builder
import org.json.JSONObject

class CollectorCookie {
    val cookie: Cookie

    constructor(cookie: Cookie) {
        this.cookie = cookie
    }

    constructor(serialized: String) {
        val `object` = JSONObject(serialized)
        cookie = Builder()
            .name(`object`.getString("name"))
            .value(`object`.getString("value"))
            .expiresAt(`object`.getLong("expiresAt"))
            .domain(`object`.getString("domain"))
            .path(`object`.getString("path"))
            .build()
    }

    val isExpired: Boolean
        get() = cookie.expiresAt < System.currentTimeMillis()
    
    val cookieKey: String
        get() = (if (cookie.secure) "https" else "http") + "://" + cookie.domain + cookie.path + "|" + cookie.name

    fun serialize(): String {
        val values = HashMap<String?, Any?>()
        values["name"] = cookie.name
        values["value"] = cookie.value
        values["expiresAt"] = cookie.expiresAt
        values["domain"] = cookie.domain
        values["path"] = cookie.path
        return JSONObject(values).toString()
    }

    override fun equals(other: Any?): Boolean {
        if (other !is CollectorCookie) return false

        return other.cookie.name == cookie.name 
                && other.cookie.domain == cookie.domain 
                && other.cookie.path == cookie.path
    }

    override fun hashCode(): Int {
        var hash = 17
        hash = 31 * hash + cookie.name.hashCode()
        hash = 31 * hash + cookie.domain.hashCode()
        hash = 31 * hash + cookie.path.hashCode()
        return hash
    }

    companion object {
        fun decorateAll(cookies: Collection<Cookie>): List<CollectorCookie> {
            val collectorCookies: MutableList<CollectorCookie> = ArrayList(cookies.size)
            for (cookie in cookies) {
                collectorCookies.add(CollectorCookie(cookie))
            }
            return collectorCookies
        }
    }
}
