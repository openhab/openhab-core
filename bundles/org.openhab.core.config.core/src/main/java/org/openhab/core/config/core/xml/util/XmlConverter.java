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
 * The {@link XmlConverter} converts the XML element the {@link XmlNodeReader} is currently positioned at into an
 * object.
 * <p>
 * Converters are registered at the {@link XmlMapping} of an {@link XmlDocumentReader}.
 *
 * @author Holger Friedrich - Initial contribution
 */
@NonNullByDefault
public interface XmlConverter {

    /**
     * Returns whether this converter can create objects of the specified type.
     *
     * @param type the type to be checked
     * @return true if this converter can create objects of the specified type
     */
    boolean canConvert(Class<?> type);

    /**
     * Converts the XML element the reader is currently positioned at.
     *
     * @param reader the reader positioned at the XML element to be converted
     * @param context the context of the current conversion
     * @return the conversion result (could be null)
     * @throws ConversionException if the XML element contains invalid content
     */
    @Nullable
    Object unmarshal(XmlNodeReader reader, XmlUnmarshallingContext context) throws ConversionException;
}
