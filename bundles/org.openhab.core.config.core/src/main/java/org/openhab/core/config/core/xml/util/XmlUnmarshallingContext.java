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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * The {@link XmlUnmarshallingContext} is passed to the {@link XmlConverter}s during the conversion of one XML document.
 * <p>
 * It converts nested elements and holds data which is shared between the converters of the document.
 *
 * @author Holger Friedrich - Initial contribution
 */
@NonNullByDefault
public final class XmlUnmarshallingContext {

    private final XmlNodeReader reader;
    private final XmlMapping mapping;
    private final Map<String, @Nullable Object> data = new HashMap<>();

    XmlUnmarshallingContext(XmlNodeReader reader, XmlMapping mapping) {
        this.reader = reader;
        this.mapping = mapping;
    }

    /**
     * Converts the XML element the reader is currently positioned at into an object of the specified type.
     * <p>
     * If a converter is registered for the type, it is used. Otherwise, if the type is {@link List}, all unread child
     * elements are converted into a list. The type of each child element is determined by its alias.
     *
     * @param type the type of the result
     * @return the conversion result (could be null)
     * @throws ConversionException if no converter can be found or the XML element contains invalid content
     */
    public @Nullable Object convertAnother(Class<?> type) throws ConversionException {
        XmlConverter converter = mapping.lookupConverter(type);
        if (converter != null) {
            try {
                return converter.unmarshal(reader, this);
            } catch (ConversionException e) {
                throw e;
            } catch (RuntimeException e) {
                throw new ConversionException("The node '" + reader.getNodeName() + "' could not be converted: " + e,
                        e);
            }
        }
        if (type.isAssignableFrom(ArrayList.class)) {
            List<@Nullable Object> list = new ArrayList<>();
            while (reader.hasMoreChildren()) {
                reader.moveDown();
                list.add(convertAnother(mapping.resolveAlias(reader.getNodeName())));
                reader.moveUp();
            }
            return list;
        }
        throw new ConversionException("No converter is registered for type '" + type.getName() + "'!");
    }

    /**
     * Returns the data stored with the specified key.
     *
     * @param key the key of the data
     * @return the data or null if no data is stored with the key
     */
    public @Nullable Object get(String key) {
        return data.get(key);
    }

    /**
     * Stores the specified data with the specified key, so that it is available for all converters of the current
     * document.
     *
     * @param key the key of the data
     * @param value the data to be stored
     */
    public void put(String key, @Nullable Object value) {
        data.put(key, value);
    }
}
