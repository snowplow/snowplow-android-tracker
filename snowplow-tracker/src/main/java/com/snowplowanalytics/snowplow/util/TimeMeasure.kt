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
        
        val that = other as TimeMeasure
        val ms1 = convert(TimeUnit.MILLISECONDS)
        val ms2 = that.convert(TimeUnit.MILLISECONDS)
        
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
