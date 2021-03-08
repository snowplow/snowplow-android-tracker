package com.snowplowanalytics.snowplow.util;

import androidx.annotation.NonNull;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class TimeMeasure {

    public final long value;
    public final TimeUnit unit;

    public TimeMeasure(long value, @NonNull TimeUnit unit) {
        Objects.requireNonNull(unit);
        this.value = value;
        this.unit = unit;
    }

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
