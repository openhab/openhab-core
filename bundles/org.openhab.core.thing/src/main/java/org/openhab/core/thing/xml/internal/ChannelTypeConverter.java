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

import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.core.ConfigDescription;
import org.openhab.core.config.core.xml.util.ConverterAttributeMapValidator;
import org.openhab.core.config.core.xml.util.NodeIterator;
import org.openhab.core.config.core.xml.util.NodeValue;
import org.openhab.core.thing.type.AutoUpdatePolicy;
import org.openhab.core.thing.type.ChannelKind;
import org.openhab.core.thing.type.ChannelType;
import org.openhab.core.thing.type.ChannelTypeBuilder;
import org.openhab.core.thing.type.ChannelTypeUID;
import org.openhab.core.thing.type.TriggerChannelTypeBuilder;
import org.openhab.core.types.CommandDescription;
import org.openhab.core.types.EventDescription;
import org.openhab.core.types.StateDescription;
import org.openhab.core.types.StateDescriptionFragment;
import org.openhab.core.types.StateDescriptionFragmentBuilder;

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
 * @author Michael Grammling - Initial contribution
 * @author Ivan Iliev - Added support for system wide channel types
 */
@NonNullByDefault
public class ChannelTypeConverter extends AbstractDescriptionTypeConverter<ChannelTypeXmlResult> {

    public ChannelTypeConverter() {
        super(ChannelTypeXmlResult.class, "channel-type");
        attributeMapValidator = new ConverterAttributeMapValidator(
                new String[][] { { "id", "true" }, { "advanced", "false" }, { "system", "false" } });
    }

    private boolean readBoolean(Map<String, String> attributes, String attributeName, boolean defaultValue) {
        String advancedFlag = attributes.get(attributeName);

        if (advancedFlag != null) {
            return Boolean.parseBoolean(advancedFlag);
        }

        return defaultValue;
    }

    private @Nullable String readItemType(NodeIterator nodeIterator) throws ConversionException {
        return (String) nodeIterator.nextValue("item-type", false);
    }

    private @Nullable String readKind(NodeIterator nodeIterator) throws ConversionException {
        return (String) nodeIterator.nextValue("kind", false);
    }

    private @Nullable String readCategory(NodeIterator nodeIterator) throws ConversionException {
        return (String) nodeIterator.nextValue("category", false);
    }

    private @Nullable AutoUpdatePolicy readAutoUpdatePolicy(NodeIterator nodeIterator) {
        String string = (String) nodeIterator.nextValue("autoUpdatePolicy", false);
        if (string != null) {
            return AutoUpdatePolicy.valueOf(string.toUpperCase(Locale.ENGLISH));
        }
        return null;
    }

    private @Nullable Set<String> readTags(NodeIterator nodeIterator) throws ConversionException {
        Set<String> tags = null;

        List<@NonNull ?> tagsNode = nodeIterator.nextList("tags", false);

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

    private @Nullable StateDescription readStateDescription(NodeIterator nodeIterator) {
        Object nextNode = nodeIterator.next();

        if (nextNode != null) {
            if (nextNode instanceof StateDescription) {
                return (StateDescription) nextNode;
            }

            nodeIterator.revert();
        }

        return null;
    }

    private @Nullable EventDescription readEventDescription(NodeIterator nodeIterator) {
        Object nextNode = nodeIterator.next();

        if (nextNode != null) {
            if (nextNode instanceof EventDescription) {
                return (EventDescription) nextNode;
            }

            nodeIterator.revert();
        }

        return null;
    }

    private @Nullable CommandDescription readCommandDescription(NodeIterator nodeIterator) throws ConversionException {
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
    protected @Nullable ChannelTypeXmlResult unmarshalType(HierarchicalStreamReader reader,
            UnmarshallingContext context, Map<String, String> attributes, NodeIterator nodeIterator)
            throws ConversionException {
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
        StateDescriptionFragment stateDescriptionFragment = stateDescription != null
                ? StateDescriptionFragmentBuilder.create(stateDescription).build()
                : null;

        CommandDescription commandDescription = readCommandDescription(nodeIterator);
        EventDescription eventDescription = readEventDescription(nodeIterator);

        AutoUpdatePolicy autoUpdatePolicy = readAutoUpdatePolicy(nodeIterator);

        Object[] configDescriptionObjects = super.getConfigDescriptionObjects(nodeIterator);

        if (kind == null) {
            // Default for kind is 'state'
            kind = ChannelKind.STATE.name();
        }

        ChannelKind cKind = ChannelKind.parse(kind);
        URI configDescriptionURI = (URI) configDescriptionObjects[0];
        final ChannelTypeBuilder<?> builder;
        if (cKind == ChannelKind.STATE) {
            itemType = requireNonEmpty(itemType, "ChannelType 'itemType' must not be null or empty.");
            builder = ChannelTypeBuilder.state(channelTypeUID, label, itemType).isAdvanced(advanced)
                    .withConfigDescriptionURI(configDescriptionURI)
                    .withStateDescriptionFragment(stateDescriptionFragment).withAutoUpdatePolicy(autoUpdatePolicy)
                    .withCommandDescription(commandDescription);
        } else if (cKind == ChannelKind.TRIGGER) {
            TriggerChannelTypeBuilder triggerChannelTypeBuilder = ChannelTypeBuilder.trigger(channelTypeUID, label)
                    .isAdvanced(advanced).withConfigDescriptionURI(configDescriptionURI);
            builder = triggerChannelTypeBuilder;
            if (eventDescription != null) {
                triggerChannelTypeBuilder.withEventDescription(eventDescription);
            }
        } else {
            throw new IllegalArgumentException(String.format("Unknown channel kind: '%s'", cKind));
        }
        if (category != null) {
            builder.withCategory(category);
        }
        if (description != null) {
            builder.withDescription(description);
        }
        if (tags != null) {
            builder.withTags(tags);
        }
        ChannelType channelType = builder.build();

        return new ChannelTypeXmlResult(channelType, (ConfigDescription) configDescriptionObjects[1], system);
    }
}
