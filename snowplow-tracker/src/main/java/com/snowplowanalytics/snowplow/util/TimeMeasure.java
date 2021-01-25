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
}
