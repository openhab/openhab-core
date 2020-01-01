/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.core.config.xml.util;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;

/**
 * The {@link ConverterAssertion} class contains utility methods to assure certain conditions.
 * If any of the conditions do <i>not</i> fit, then a {@link ConversionException} is thrown.
 * <p>
 * <b>Hint:</b> This class cannot be instantiated, it contains only static methods.
 *
 * @author Michael Grammling - Initial contribution
 */
public class ConverterAssertion {

    private ConverterAssertion() {
        // nothing to do
    }

    /**
     * Asserts that the specified property is neither {@code null} nor empty.
     *
     * @param propertyName the name of the property to be checked (must neither be null, nor empty)
     * @param property the property to be checked (could be null or empty)
     * @throws ConversionException if the condition does not fit
     */
    public static void assertNeitherNullNorEmpty(String propertyName, String property) throws ConversionException {
        if ((property == null) || (property.isEmpty())) {
            throw new ConversionException("The " + propertyName + " must neither be null nor empty!");
        }
    }

    /**
     * Asserts that the specified reader does <i>not</i> contain further elements in its section.
     *
     * @param reader the reader to be used for validation (must not be null)
     * @throws ConversionException if the condition does not fit
     */
    public static void assertEndOfType(HierarchicalStreamReader reader) throws ConversionException {
        if (reader.hasMoreChildren()) {
            throw new ConversionException("The document is invalid, it contains unsupported data!");
        }
    }

    /**
     * Asserts that the current node associated with the specified reader does <i>not</i> contain
     * any attributes.
     *
     * @param reader the reader to be used for validation (must not be null)
     * @throws ConversionException if the condition does not fit
     */
    public static void assertNoAttribute(HierarchicalStreamReader reader) throws ConversionException {
        if (reader.getAttributeCount() > 0) {
            throw new ConversionException("The parameter '" + reader.getNodeName() + "' uses unknown attributes!");
        }
    }

}
