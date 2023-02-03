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

import android.content.Context
import android.content.SharedPreferences
import com.snowplowanalytics.core.constants.TrackerConstants
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import org.json.JSONException
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class CollectorCookieJar(context: Context) : CookieJar {
    private val cookies: MutableSet<CollectorCookie> = Collections.newSetFromMap(ConcurrentHashMap())
    private val sharedPreferences: SharedPreferences

    init {
        sharedPreferences =
            context.getSharedPreferences(TrackerConstants.COOKIE_PERSISTANCE, Context.MODE_PRIVATE)
        loadFromSharedPreferences()
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val cookiesToRemove: MutableList<CollectorCookie> = ArrayList()
        val validCookies: MutableList<Cookie> = ArrayList()
        
        for (currentCookie in cookies) {
            if (currentCookie.isExpired) {
                cookiesToRemove.add(currentCookie)
            } else if (currentCookie.cookie.matches(url)) {
                validCookies.add(currentCookie.cookie)
            }
        }
        if (cookiesToRemove.isNotEmpty()) {
            removeAll(cookiesToRemove)
        }
        return validCookies
    }

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        saveAll(cookies)
    }

    fun clear() {
        sharedPreferences.edit().clear().apply()
        cookies.clear()
    }

    private fun loadFromSharedPreferences() {
        val cookiesToRemove: MutableList<String> = ArrayList()
        for ((key, value) in sharedPreferences.all) {
            val serializedCookie = value as? String? ?: continue
            try {
                val cookie = CollectorCookie(serializedCookie)
                cookies.add(cookie)
            } catch (ignored: JSONException) {
                cookiesToRemove.add(key)
            }
        }
        
        if (cookiesToRemove.isNotEmpty()) {
            val editor = sharedPreferences.edit()
            for (cookie in cookiesToRemove) {
                editor.remove(cookie)
            }
            editor.apply()
        }
    }

    private fun saveAll(newCookies: Collection<Cookie>) {
        val editor = sharedPreferences.edit()
        for (cookie in CollectorCookie.decorateAll(newCookies)) {
            cookies.remove(cookie)
            cookies.add(cookie)
            editor.putString(cookie.cookieKey, cookie.serialize())
        }
        editor.apply()
    }

    private fun removeAll(cookiesToRemove: Collection<CollectorCookie>) {
        val editor = sharedPreferences.edit()
        for (cookie in cookiesToRemove) {
            cookies.remove(cookie)
            editor.remove(cookie.cookieKey)
        }
        editor.apply()
    }
}
