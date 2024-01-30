/*
 * Copyright (c) 2015-present Snowplow Analytics Ltd. All rights reserved.
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
package com.snowplowanalytics.core.utils

import java.lang.ref.WeakReference
import java.util.*

object NotificationCenter {
    private val notificationMap: MutableMap<String, MutableList<WeakObserver>> = HashMap()
    private val observerMap = WeakHashMap<FunctionalObserver, WeakObserver>()
    
    @JvmStatic
    @Synchronized
    fun addObserver(notificationType: String, observer: FunctionalObserver) {
        val weakObserver = WeakObserver(observer)
        val previousObserver = observerMap.put(observer, weakObserver)
        previousObserver?.invalidate()
        var observers = notificationMap[notificationType]
        if (observers == null) {
            observers = LinkedList()
            notificationMap[notificationType] = observers
        }
        observers.add(weakObserver)
    }

    @JvmStatic
    @Synchronized
    fun removeObserver(observer: FunctionalObserver): Boolean {
        val weakObserver = observerMap.remove(observer)
        if (weakObserver != null) {
            weakObserver.invalidate()
            return true
        }
        return false
    }

    @JvmStatic
    @Synchronized
    fun removeAll() {
        observerMap.clear()
        notificationMap.clear()
    }

    @JvmStatic
    @Synchronized
    fun postNotification(notificationType: String, data: Map<String, Any>): Boolean {
        val observers = notificationMap[notificationType]
        if (observers == null || observers.isEmpty()) {
            return false
        }
        val iterator = observers.iterator()
        while (iterator.hasNext()) {
            val weakObserver = iterator.next()
            if (!weakObserver.isValid()) {
                synchronized(NotificationCenter::class.java) { iterator.remove() }
                continue
            }
            val dataCopy: Map<String, Any> = HashMap(data)
            weakObserver.get()?.apply(dataCopy)
        }
        return observers.isNotEmpty()
    }

    abstract class FunctionalObserver {
        abstract fun apply(data: Map<String, Any>)
    }

    private class WeakObserver(referent: FunctionalObserver?) :
        WeakReference<FunctionalObserver?>(referent) {
        
        private var valid = true
        
        @Synchronized
        fun isValid(): Boolean {
            return valid && get() != null
        }

        @Synchronized
        fun invalidate() {
            valid = false
            clear()
        }
    }
}
