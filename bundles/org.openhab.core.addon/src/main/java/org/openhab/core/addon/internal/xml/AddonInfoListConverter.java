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
import org.openhab.core.addon.AddonInfo;
import org.openhab.core.addon.AddonInfoList;
import org.openhab.core.config.core.xml.util.GenericUnmarshaller;
import org.openhab.core.config.core.xml.util.NodeIterator;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;

/**
 * The {@link AddonInfoListConverter} is a concrete implementation of the {@code XStream} {@link Converter}
 * interface used to convert a list of add-on information within an XML document into a list of {@link AddonInfo}
 * objects.
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
@NonNullByDefault
public class AddonInfoListConverter extends GenericUnmarshaller<AddonInfoList> {

    public AddonInfoListConverter() {
        super(AddonInfoList.class);
    }

    @Override
    public @Nullable Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
        List<?> nodes = (List<?>) context.convertAnother(context, List.class);
        NodeIterator nodeIterator = new NodeIterator(nodes);

        Object object = nodeIterator.nextList("addons", false);
        List<AddonInfo> addons = (object instanceof List<?> list)
                ? list.stream().filter(e -> e != null).filter(e -> (e instanceof AddonInfoXmlResult))
                        .map(e -> (AddonInfoXmlResult) e).map(r -> r.addonInfo()).toList()
                : null;

        nodeIterator.assertEndOfType();

        return new AddonInfoList().setAddons(addons);
    }
}
