/*
 * Copyright (c) 2015-2023 Snowplow Analytics Ltd. All rights reserved.
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
