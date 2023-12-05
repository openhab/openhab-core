/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
import org.openhab.core.addon.AddonMatchProperty;
import org.openhab.core.config.core.xml.util.GenericUnmarshaller;
import org.openhab.core.config.core.xml.util.NodeIterator;

import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;

/**
 * The {@link AddonMatchPropertyConverter} is a concrete implementation of the {@code XStream} {@link Converter}
 * interface used to convert add-on discovery method match property information within an XML document into a
 * {@link AddonMatchProperty} object.
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
@NonNullByDefault
public class AddonMatchPropertyConverter extends GenericUnmarshaller<AddonMatchProperty> {

    public AddonMatchPropertyConverter() {
        super(AddonMatchProperty.class);
    }

    @Override
    public @Nullable Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
        List<?> nodes = (List<?>) context.convertAnother(context, List.class);
        NodeIterator nodeIterator = new NodeIterator(nodes);

        String name = requireNonEmpty((String) nodeIterator.nextValue("name", true), "Name is null or empty");
        String regex = requireNonEmpty((String) nodeIterator.nextValue("regex", true), "Regex is null or empty");

        nodeIterator.assertEndOfType();

        return new AddonMatchProperty(name, regex);
    }
}
