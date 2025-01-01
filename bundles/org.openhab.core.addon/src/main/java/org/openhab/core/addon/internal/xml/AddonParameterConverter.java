/**
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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
package org.openhab.core.addon.internal.xml;

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.addon.AddonParameter;
import org.openhab.core.config.core.xml.util.GenericUnmarshaller;
import org.openhab.core.config.core.xml.util.NodeIterator;

import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;

/**
 * The {@link AddonParameterConverter} is a concrete implementation of the {@code XStream}
 * {@link com.thoughtworks.xstream.converters.Converter}
 * interface used to convert add-on discovery method parameter information within an XML document into a
 * {@link org.openhab.core.addon.AddonMatchProperty} object.
 *
 * @author Mark Herwege - Initial contribution
 */
@NonNullByDefault
public class AddonParameterConverter extends GenericUnmarshaller<AddonParameter> {

    public AddonParameterConverter() {
        super(AddonParameter.class);
    }

    @Override
    public @Nullable Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
        List<?> nodes = (List<?>) context.convertAnother(context, List.class);
        NodeIterator nodeIterator = new NodeIterator(nodes);

        String name = requireNonEmpty((String) nodeIterator.nextValue("name", true), "Name is null or empty");
        String value = requireNonEmpty((String) nodeIterator.nextValue("value", true), "Value is null or empty");

        nodeIterator.assertEndOfType();

        return new AddonParameter(name, value);
    }
}
