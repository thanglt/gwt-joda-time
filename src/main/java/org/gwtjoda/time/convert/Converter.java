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
package org.gwtjoda.time.convert;

/**
 * Basic converter interface for specifying what object type can be converted.
 *
 * @author Brian S O'Neill
 * @since 1.0
 */
public interface Converter {
    /**
     * Returns the object type that this converter supports, which may
     * specified by a class, superclass, abstract class, interface, or null.
     * 
     * @return the object type that this converter supports
     */
    Class getSupportedType();
}
