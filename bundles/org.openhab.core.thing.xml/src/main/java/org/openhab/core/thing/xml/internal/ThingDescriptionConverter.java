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
package org.openhab.core.thing.xml.internal;

import java.util.List;
import java.util.Map;

import org.openhab.core.config.xml.util.ConverterAttributeMapValidator;
import org.openhab.core.config.xml.util.GenericUnmarshaller;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;

/**
 * The {@link ThingDescriptionConverter} is a concrete implementation of the {@code XStream} {@link Converter} interface
 * used to convert a list of type information within an XML document
 * into a {@link ThingDescriptionList} object.
 * <p>
 * This converter converts {@code thing-descriptions} XML tags.
 *
 * @author Michael Grammling - Initial contribution
 */
public class ThingDescriptionConverter extends GenericUnmarshaller<ThingDescriptionList> {

    private ConverterAttributeMapValidator attributeMapValidator;

    public ThingDescriptionConverter() {
        super(ThingDescriptionList.class);

        this.attributeMapValidator = new ConverterAttributeMapValidator(
                new String[][] { { "bindingId", "true" }, { "schemaLocation", "false" } });
    }

    @Override
    public final Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
        // read attributes
        Map<String, String> attributes = this.attributeMapValidator.readValidatedAttributes(reader);
        String bindingId = attributes.get("bindingId");

        context.put("thing-descriptions.bindingId", bindingId);

        List<?> typeList = (List<?>) context.convertAnother(context, List.class);

        return new ThingDescriptionList(typeList);
    }
}
