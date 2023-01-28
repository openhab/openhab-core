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
package org.openhab.core.thing.xml.internal;

import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.xml.util.ConverterAttributeMapValidator;
import org.openhab.core.config.xml.util.GenericUnmarshaller;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;

/**
 * The {@link ItemTypeConverter} is a concrete implementation of the {@code XStream} {@link Converter} interface
 * used to convert an item-type within an XML document
 * into a {@link ItemType} object.
 * <p>
 * This converter converts {@code item-type} XML tags.
 *
 * @author Michael Grammling - Initial contribution
 */
@NonNullByDefault
public class ItemTypeConverter extends GenericUnmarshaller<ItemTypeConverter.ItemType> {

    public static class ItemType {
        public String itemType;
        public @Nullable String unit;

        public ItemType(String itemType, @Nullable String unit) {
            this.itemType = itemType;
            this.unit = unit;
        }
    }

    protected ConverterAttributeMapValidator attributeMapValidator;

    public ItemTypeConverter() {
        super(ItemType.class);

        this.attributeMapValidator = new ConverterAttributeMapValidator(new String[][] { { "unit", "false" } });
    }

    @Override
    public final @Nullable Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
        Map<String, String> attributes = this.attributeMapValidator.readValidatedAttributes(reader);

        String unit = attributes.get("unit");
        String type = reader.getValue();

        return new ItemType(type, unit);
    }
}
