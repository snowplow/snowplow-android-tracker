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
package com.snowplowanalytics.snowplow.util

import java.util.concurrent.TimeUnit

/**
 * It represents time durations and provides utility methods to convert across time units.
 * @param value Time duration at the selected TimeUnit.
 * @param unit Measure unit of the time duration.
 */
class TimeMeasure(val value: Long, val unit: TimeUnit) : java.io.Serializable {

    /**
     * Convert the time unit of current time duration.
     * @param toUnit The new measure unit.
     * @return The same time duration converted to the new measure unit.
     */
    fun convert(toUnit: TimeUnit): Long {
        return toUnit.convert(value, unit)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        
        val that = other as? TimeMeasure
        val ms1 = convert(TimeUnit.MILLISECONDS)
        val ms2 = that?.convert(TimeUnit.MILLISECONDS)
        
        return ms1 == ms2
    }

    override fun hashCode(): Int {
        var result = (value xor (value ushr 32)).toInt()
        result = 31 * result + unit.hashCode()
        return result
    }

    override fun toString(): String {
        return "TimeMeasure{" +
                "value=" + value +
                ", unit=" + unit +
                '}'
    }
}
