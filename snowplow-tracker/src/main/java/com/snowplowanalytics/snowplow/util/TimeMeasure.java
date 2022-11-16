package com.snowplowanalytics.snowplow.util;

import androidx.annotation.NonNull;

import java.io.Serializable;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * It represents time durations and provides utility methods to convert across time units.
 */
public class TimeMeasure implements Serializable {

    /** Time duration at the selected TimeUnit. */
    public final long value;
    /** Measure unit of the time duration. */
    public final TimeUnit unit;

    /**
     * Create an object that represent a time duration at a specific time unit.
     * @param value Time duration at the selected TimeUnit.
     * @param unit Measure unit of the time duration.
     */
    public TimeMeasure(long value, @NonNull TimeUnit unit) {
        Objects.requireNonNull(unit);
        this.value = value;
        this.unit = unit;
    }

    /**
     * Convert the time unit of current time duration.
     * @param toUnit The new measure unit.
     * @return The same time duration converted to the new measure unit.
     */
    public long convert(@NonNull TimeUnit toUnit) {
        return toUnit.convert(value, unit);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        TimeMeasure that = (TimeMeasure) o;
        long ms1 = convert(TimeUnit.MILLISECONDS);
        long ms2 = that.convert(TimeUnit.MILLISECONDS);

        return ms1 == ms2;
    }

    @Override
    public int hashCode() {
        int result = (int) (value ^ (value >>> 32));
        result = 31 * result + unit.hashCode();
        return result;
    }

    @NonNull
    @Override
    public String toString() {
        return "TimeMeasure{" +
                "value=" + value +
                ", unit=" + unit +
                '}';
    }
}
