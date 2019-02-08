/**
 * Copyright (c) 2014,2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.core.thing.xml.internal;

import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.eclipse.smarthome.config.core.ConfigDescription;
import org.eclipse.smarthome.config.xml.util.ConverterAttributeMapValidator;
import org.eclipse.smarthome.config.xml.util.NodeIterator;
import org.eclipse.smarthome.config.xml.util.NodeValue;
import org.eclipse.smarthome.core.thing.type.AutoUpdatePolicy;
import org.eclipse.smarthome.core.thing.type.ChannelKind;
import org.eclipse.smarthome.core.thing.type.ChannelType;
import org.eclipse.smarthome.core.thing.type.ChannelTypeBuilder;
import org.eclipse.smarthome.core.thing.type.ChannelTypeUID;
import org.eclipse.smarthome.core.thing.type.StateChannelTypeBuilder;
import org.eclipse.smarthome.core.types.CommandDescription;
import org.eclipse.smarthome.core.types.EventDescription;
import org.eclipse.smarthome.core.types.StateDescription;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;

/**
 * The {@link ChannelTypeConverter} is a concrete implementation of the {@code XStream} {@link Converter} interface used
 * to convert channel type information within an XML document
 * into a {@link ChannelTypeXmlResult} object.
 * <p>
 * This converter converts {@code channel-type} XML tags. It uses the {@link AbstractDescriptionTypeConverter} which
 * offers base functionality for each type definition.
 *
 * @author Michael Grammling - Initial Contribution
 * @author Ivan Iliev - Added support for system wide channel types
 */
public class ChannelTypeConverter extends AbstractDescriptionTypeConverter<ChannelTypeXmlResult> {

    public ChannelTypeConverter() {
        super(ChannelTypeXmlResult.class, "channel-type");

        super.attributeMapValidator = new ConverterAttributeMapValidator(
                new String[][] { { "id", "true" }, { "advanced", "false" }, { "system", "false" } });
    }

    private boolean readBoolean(Map<String, String> attributes, String attributeName, boolean defaultValue) {
        String advancedFlag = attributes.get(attributeName);

        if (advancedFlag != null) {
            return Boolean.parseBoolean(advancedFlag);
        }

        return defaultValue;
    }

    private String readItemType(NodeIterator nodeIterator) throws ConversionException {
        return (String) nodeIterator.nextValue("item-type", false);
    }

    private String readKind(NodeIterator nodeIterator) throws ConversionException {
        return (String) nodeIterator.nextValue("kind", false);
    }

    private String readCategory(NodeIterator nodeIterator) throws ConversionException {
        return (String) nodeIterator.nextValue("category", false);
    }

    private AutoUpdatePolicy readAutoUpdatePolicy(NodeIterator nodeIterator) {
        String string = (String) nodeIterator.nextValue("autoUpdatePolicy", false);
        if (string != null) {
            return AutoUpdatePolicy.valueOf(string.toUpperCase(Locale.ENGLISH));
        }
        return null;
    }

    private Set<String> readTags(NodeIterator nodeIterator) throws ConversionException {
        Set<String> tags = null;

        List<?> tagsNode = nodeIterator.nextList("tags", false);

        if (tagsNode != null) {
            tags = new HashSet<>(tagsNode.size());

            for (Object tagNodeObject : tagsNode) {
                NodeValue tagNode = (NodeValue) tagNodeObject;

                if ("tag".equals(tagNode.getNodeName())) {
                    String tag = (String) tagNode.getValue();

                    if (tag != null) {
                        tags.add(tag);
                    }
                } else {
                    throw new ConversionException("The 'tags' node must only contain 'tag' nodes!");
                }
            }
        }

        return tags;
    }

    private StateDescription readStateDescription(NodeIterator nodeIterator) {
        Object nextNode = nodeIterator.next();

        if (nextNode != null) {
            if (nextNode instanceof StateDescription) {
                return (StateDescription) nextNode;
            }

            nodeIterator.revert();
        }

        return null;
    }

    private EventDescription readEventDescription(NodeIterator nodeIterator) {
        Object nextNode = nodeIterator.next();

        if (nextNode != null) {
            if (nextNode instanceof EventDescription) {
                return (EventDescription) nextNode;
            }

            nodeIterator.revert();
        }

        return null;
    }

    private CommandDescription readCommandDescription(NodeIterator nodeIterator) throws ConversionException {
        Object nextNode = nodeIterator.next();

        if (nextNode != null) {
            if (nextNode instanceof CommandDescription) {
                return (CommandDescription) nextNode;
            }

            nodeIterator.revert();
        }

        return null;
    }

    @Override
    protected ChannelTypeXmlResult unmarshalType(HierarchicalStreamReader reader, UnmarshallingContext context,
            Map<String, String> attributes, NodeIterator nodeIterator) throws ConversionException {
        boolean advanced = readBoolean(attributes, "advanced", false);
        boolean system = readBoolean(attributes, "system", false);

        String uid = system ? XmlHelper.getSystemUID(super.getID(attributes)) : super.getUID(attributes, context);
        ChannelTypeUID channelTypeUID = new ChannelTypeUID(uid);

        String itemType = readItemType(nodeIterator);
        String kind = readKind(nodeIterator);
        String label = super.readLabel(nodeIterator);
        String description = super.readDescription(nodeIterator);
        String category = readCategory(nodeIterator);
        Set<String> tags = readTags(nodeIterator);

        StateDescription stateDescription = readStateDescription(nodeIterator);
        CommandDescription commandDescription = readCommandDescription(nodeIterator);
        EventDescription eventDescription = readEventDescription(nodeIterator);

        AutoUpdatePolicy autoUpdatePolicy = readAutoUpdatePolicy(nodeIterator);

        Object[] configDescriptionObjects = super.getConfigDescriptionObjects(nodeIterator);

        if (kind == null) {
            // Default for kind is 'state'
            kind = "state";
        }

        ChannelKind cKind = ChannelKind.parse(kind);
        URI configDescriptionURI = (URI) configDescriptionObjects[0];
        ChannelType channelType = null;
        if (cKind == ChannelKind.STATE) {
            StateChannelTypeBuilder builder = ChannelTypeBuilder.state(channelTypeUID, label, itemType)
                    .isAdvanced(advanced).withDescription(description).withCategory(category).withTags(tags)
                    .withConfigDescriptionURI(configDescriptionURI).withStateDescription(stateDescription)
                    .withAutoUpdatePolicy(autoUpdatePolicy).withCommandDescription(commandDescription);
            channelType = builder.build();
        } else if (cKind == ChannelKind.TRIGGER) {
            channelType = ChannelTypeBuilder.trigger(channelTypeUID, label).isAdvanced(advanced)
                    .withDescription(description).withCategory(category).withTags(tags)
                    .withConfigDescriptionURI(configDescriptionURI).withEventDescription(eventDescription).build();
        }

        ChannelTypeXmlResult channelTypeXmlResult = new ChannelTypeXmlResult(channelType,
                (ConfigDescription) configDescriptionObjects[1], system);

        return channelTypeXmlResult;
    }

}
