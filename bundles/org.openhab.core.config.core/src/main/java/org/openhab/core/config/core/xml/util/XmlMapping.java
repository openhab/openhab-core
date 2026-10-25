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
 * The {@link XmlMapping} maps XML element names to types (aliases) and types to {@link XmlConverter}s.
 * <p>
 * Only elements with a registered alias can be converted. No types are instantiated via reflection.
 *
 * @author Holger Friedrich - Initial contribution
 */
@NonNullByDefault
public final class XmlMapping {

    private final Map<String, Class<?>> aliases = new HashMap<>();
    private final List<XmlConverter> converters = new ArrayList<>();

    XmlMapping() {
    }

    /**
     * Maps the XML element with the specified name to the specified type. If the name is already mapped, the previous
     * mapping is replaced.
     *
     * @param name the name of the XML element
     * @param type the type the XML element is converted into
     */
    public void alias(String name, Class<?> type) {
        aliases.put(name, type);
    }

    /**
     * Registers the specified converter. If multiple converters can convert the same type, the converter registered
     * last is used.
     *
     * @param converter the converter to be registered
     */
    public void registerConverter(XmlConverter converter) {
        converters.add(converter);
    }

    @Nullable
    Class<?> lookupAlias(String name) {
        return aliases.get(name);
    }

    Class<?> resolveAlias(String name) throws ConversionException {
        Class<?> type = aliases.get(name);
        if (type == null) {
            throw new ConversionException("The node '" + name + "' is not supported!");
        }
        return type;
    }

    @Nullable
    XmlConverter lookupConverter(Class<?> type) {
        for (int i = converters.size() - 1; i >= 0; i--) {
            XmlConverter converter = converters.get(i);
            if (converter.canConvert(type)) {
                return converter;
            }
        }
        return null;
    }
}
