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
package org.gwtjoda.time.field;

import org.gwtjoda.time.DateTimeField;
import org.gwtjoda.time.DateTimeFieldType;
import org.gwtjoda.time.DurationField;

/**
 * <code>DecoratedDateTimeField</code> extends {@link BaseDateTimeField},
 * implementing only the minimum required set of methods. These implemented
 * methods delegate to a wrapped field.
 * <p>
 * This design allows new DateTimeField types to be defined that piggyback on
 * top of another, inheriting all the safe method implementations from
 * BaseDateTimeField. Should any method require pure delegation to the
 * wrapped field, simply override and use the provided getWrappedField method.
 * <p>
 * DecoratedDateTimeField is thread-safe and immutable, and its subclasses must
 * be as well.
 *
 * @author Brian S O'Neill
 * @since 1.0
 * @see DelegatedDateTimeField
 */
public abstract class DecoratedDateTimeField extends BaseDateTimeField {

    /** Serialization version */
    private static final long serialVersionUID = 203115783733757597L;

    /** The DateTimeField being wrapped */
    private final DateTimeField iField;

    /**
     * Constructor.
     * 
     * @param field  the field being decorated
     * @param type  allow type to be overridden
     */
    protected DecoratedDateTimeField(DateTimeField field, DateTimeFieldType type) {
        super(type);
        if (field == null) {
            throw new IllegalArgumentException("The field must not be null");
        }
        if (!field.isSupported()) {
            throw new IllegalArgumentException("The field must be supported");
        }
        iField = field;
    }

    /**
     * Gets the wrapped date time field.
     * 
     * @return the wrapped DateTimeField
     */
    public final DateTimeField getWrappedField() {
        return iField;
    }

    public boolean isLenient() {
        return iField.isLenient();
    }

    public int get(long instant) {
        return iField.get(instant);
    }

    public long set(long instant, int value) {
        return iField.set(instant, value);
    }

    public DurationField getDurationField() {
        return iField.getDurationField();
    }

    public DurationField getRangeDurationField() {
        return iField.getRangeDurationField();
    }

    public int getMinimumValue() {
        return iField.getMinimumValue();
    }

    public int getMaximumValue() {
        return iField.getMaximumValue();
    }

    public long roundFloor(long instant) {
        return iField.roundFloor(instant);
    }

}
