/*
 *  Copyright 2001-2005 Stephen Colebourne
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.gwtjoda.time.chrono;


import java.util.Locale;

import org.gwtjoda.time.DateTimeConstants;
import org.gwtjoda.time.DateTimeFieldType;
import org.gwtjoda.time.DurationField;
import org.gwtjoda.time.DurationFieldType;
import org.gwtjoda.time.field.BaseDateTimeField;
import org.gwtjoda.time.field.FieldUtils;
import org.gwtjoda.time.field.UnsupportedDurationField;

/**
 * Provides time calculations for the era component of time.
 *
 * @author Stephen Colebourne
 * @author Brian S O'Neill
 * @since 1.0
 */
final class GJEraDateTimeField extends BaseDateTimeField {
    
    /** Serialization version */
    private static final long serialVersionUID = 4240986525305515528L;

    private final BasicChronology iChronology;

    /**
     * Restricted constructor
     */
    GJEraDateTimeField(BasicChronology chronology) {
        super(DateTimeFieldType.era());
        iChronology = chronology;
    }

    public boolean isLenient() {
        return false;
    }

    /**
     * Get the Era component of the specified time instant.
     * 
     * @param instant  the time instant in millis to query.
     */
    public int get(long instant) {
        if (iChronology.getYear(instant) <= 0) {
            return DateTimeConstants.BCE;
        } else {
            return DateTimeConstants.CE;
        }
    }

    public String getAsText(int fieldValue, Locale locale) {
        return GJLocaleSymbols.forLocale(locale).eraValueToText(fieldValue);
    }

    /**
     * Set the Era component of the specified time instant.
     * 
     * @param instant  the time instant in millis to update.
     * @param era  the era to update the time to.
     * @return the updated time instant.
     * @throws IllegalArgumentException  if era is invalid.
     */
    public long set(long instant, int era) {
        FieldUtils.verifyValueBounds(this, era, DateTimeConstants.BCE, DateTimeConstants.CE);
            
        int oldEra = get(instant);
        if (oldEra != era) {
            int year = iChronology.getYear(instant);
            return iChronology.setYear(instant, -year);
        } else {
            return instant;
        }
    }

    public long set(long instant, String text, Locale locale) {
        return set(instant, GJLocaleSymbols.forLocale(locale).eraTextToValue(text));
    }

    public long roundFloor(long instant) {
        if (get(instant) == DateTimeConstants.CE) {
            return iChronology.setYear(0, 1);
        } else {
            return Long.MIN_VALUE;
        }
    }

    public long roundCeiling(long instant) {
        if (get(instant) == DateTimeConstants.BCE) {
            return iChronology.setYear(0, 1);
        } else {
            return Long.MAX_VALUE;
        }
    }

    public long roundHalfFloor(long instant) {
        // In reality, the era is infinite, so there is no halfway point.
        return roundFloor(instant);
    }

    public long roundHalfCeiling(long instant) {
        // In reality, the era is infinite, so there is no halfway point.
        return roundFloor(instant);
    }

    public long roundHalfEven(long instant) {
        // In reality, the era is infinite, so there is no halfway point.
        return roundFloor(instant);
    }

    public DurationField getDurationField() {
        return UnsupportedDurationField.getInstance(DurationFieldType.eras());
    }

    public DurationField getRangeDurationField() {
        return null;
    }

    public int getMinimumValue() {
        return DateTimeConstants.BCE;
    }

    public int getMaximumValue() {
        return DateTimeConstants.CE;
    }

    public int getMaximumTextLength(Locale locale) {
        return GJLocaleSymbols.forLocale(locale).getEraMaxTextLength();
    }

    /**
     * Serialization singleton
     */
    private Object readResolve() {
        return iChronology.era();
    }
}
