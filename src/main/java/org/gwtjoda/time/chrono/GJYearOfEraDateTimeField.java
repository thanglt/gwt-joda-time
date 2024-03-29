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

import org.gwtjoda.time.DateTimeField;
import org.gwtjoda.time.DateTimeFieldType;
import org.gwtjoda.time.ReadablePartial;
import org.gwtjoda.time.field.DecoratedDateTimeField;
import org.gwtjoda.time.field.FieldUtils;

/**
 * Provides time calculations for the year of era component of time.
 * 
 * @author Brian S O'Neill
 * @since 1.0
 */
final class GJYearOfEraDateTimeField extends DecoratedDateTimeField {

    private static final long serialVersionUID = -5961050944769862059L;

    private final BasicChronology iChronology;

    /**
     * Restricted constructor.
     */
    GJYearOfEraDateTimeField(DateTimeField yearField, BasicChronology chronology) {
        super(yearField, DateTimeFieldType.yearOfEra());
        iChronology = chronology;
    }

    public int get(long instant) {
        int year = getWrappedField().get(instant);
        if (year <= 0) {
            year = 1 - year;
        }
        return year;
    }

    public long add(long instant, int years) {
        return getWrappedField().add(instant, years);
    }

    public long add(long instant, long years) {
        return getWrappedField().add(instant, years);
    }

    public long addWrapField(long instant, int years) {
        return getWrappedField().addWrapField(instant, years);
    }

    public int[] addWrapField(ReadablePartial instant, int fieldIndex, int[] values, int years) {
        return getWrappedField().addWrapField(instant, fieldIndex, values, years);
    }

    public int getDifference(long minuendInstant, long subtrahendInstant) {
        return getWrappedField().getDifference(minuendInstant, subtrahendInstant);
    }

    public long getDifferenceAsLong(long minuendInstant, long subtrahendInstant) {
        return getWrappedField().getDifferenceAsLong(minuendInstant, subtrahendInstant);
    }

    /**
     * Set the year component of the specified time instant.
     * 
     * @param instant  the time instant in millis to update.
     * @param year  the year (0,292278994) to update the time to.
     * @return the updated time instant.
     * @throws IllegalArgumentException  if year is invalid.
     */
    public long set(long instant, int year) {
        FieldUtils.verifyValueBounds(this, year, 1, getMaximumValue());
        if (iChronology.getYear(instant) <= 0) {
            year = 1 - year;
        }
        return super.set(instant, year);
    }

    public int getMinimumValue() {
        return 1;
    }

    public int getMaximumValue() {
        return getWrappedField().getMaximumValue();
    }

    public long roundFloor(long instant) {
        return getWrappedField().roundFloor(instant);
    }

    public long roundCeiling(long instant) {
        return getWrappedField().roundCeiling(instant);
    }

    public long remainder(long instant) {
        return getWrappedField().remainder(instant);
    }

    /**
     * Serialization singleton
     */
    private Object readResolve() {
        return iChronology.yearOfEra();
    }
}
