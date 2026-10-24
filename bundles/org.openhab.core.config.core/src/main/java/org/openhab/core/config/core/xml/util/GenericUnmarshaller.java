/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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
package org.openhab.core.config.core.xml.util;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * The {@link GenericUnmarshaller} is an abstract implementation of the {@link XmlConverter} interface used
 * to convert XML tags within an XML document into its
 * according objects.
 *
 * @author Michael Grammling - Initial contribution
 *
 * @param <T> the result type of the conversion
 */
@NonNullByDefault
public abstract class GenericUnmarshaller<T> implements XmlConverter {

    private Class<T> clazz;

    /**
     * Creates a new instance of this class with the specified parameter.
     *
     * @param clazz the class of the result type (must not be null)
     */
    public GenericUnmarshaller(Class<T> clazz) {
        this.clazz = clazz;
    }

    /**
     * Returns the class of the result type
     *
     * @return the class of the result type
     */
    public Class<?> getResultType() {
        return this.clazz;
    }

    @Override
    public final boolean canConvert(Class<?> paramClass) {
        return (clazz.equals(paramClass));
    }

    /**
     * Checks that the specified object is not {@code null} and throws a customized {@link ConversionException} if it
     * is.
     *
     * @param object the object to check for nullity
     * @param message detail message to be used in the event that a {@code ConversionException} is thrown
     * @return {@code object} if not {@code null}
     * @throws ConversionException if {@code object} is {@code null}
     */
    protected static Object requireNonNull(@Nullable Object object, String message) {
        if (object == null) {
            throw new ConversionException(message);
        }
        return object;
    }

    /**
     * Checks that the specified string is not {@code null} and not empty and throws a customized
     * {@link ConversionException} if it is.
     *
     * @param string the string to check for nullity and emptiness
     * @param message detail message to be used in the event that a {@code ConversionException} is thrown
     * @return {@code string} if not {@code null} and not empty
     * @throws ConversionException if {@code string} is {@code null} or empty
     */
    protected static String requireNonEmpty(@Nullable String string, String message) {
        if (string == null || string.isEmpty()) {
            throw new ConversionException(message);
        }
        return string;
    }
}
