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
package org.openhab.core.io.websocket;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.events.Event;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemNotFoundException;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.items.events.ItemEventFactory;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.openhab.core.types.State;
import org.openhab.core.types.Type;
import org.openhab.core.types.TypeParser;
import org.openhab.core.types.UnDefType;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

/**
 * The {@link EventDTO} is used for serialization and deserialization of events
 *
 * @author Stefan Bußweiler - Initial contribution ({@link org.openhab.core.items.events.ItemEventFactory})
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
public class ItemEventUtility {
    private static final Pattern TOPIC_PATTERN = Pattern.compile("openhab/items/(?<entity>\\w+)/(?<action>\\w+)");
    private static final String TYPE_POSTFIX = "Type";

    private final Gson gson;
    private final ItemRegistry itemRegistry;

    public ItemEventUtility(Gson gson, ItemRegistry itemRegistry) {
        this.gson = gson;
        this.itemRegistry = itemRegistry;
    }

    public Event createCommandEvent(EventDTO eventDTO) throws EventProcessingException {
        Matcher matcher = getTopicMatcher(eventDTO.topic, "command");
        Item item = getItem(matcher.group("entity"));
        Type command = parseType(eventDTO.payload);
        if (command instanceof Command) {
            List<Class<? extends Command>> acceptedItemCommandTypes = item.getAcceptedCommandTypes();
            if (acceptedItemCommandTypes.contains(command.getClass())) {
                return ItemEventFactory.createCommandEvent(item.getName(), (Command) command, eventDTO.source);
            }
        }
        throw new EventProcessingException("Incompatible datatype, rejected.");
    }

    public Event createStateEvent(EventDTO eventDTO) throws EventProcessingException {
        Matcher matcher = getTopicMatcher(eventDTO.topic, "state");
        Item item = getItem(matcher.group("entity"));
        Type state = parseType(eventDTO.payload);
        if (state instanceof State) {
            List<Class<? extends State>> acceptedItemStateTypes = item.getAcceptedDataTypes();
            if (acceptedItemStateTypes.contains(state.getClass())) {
                return ItemEventFactory.createStateEvent(item.getName(), (State) state, eventDTO.source);
            }
        }
        throw new EventProcessingException("Incompatible datatype, rejected.");
    }

    private Matcher getTopicMatcher(@Nullable String topic, String action) throws EventProcessingException {
        if (topic == null) {
            throw new EventProcessingException("Topic must not be null");
        }
        Matcher matcher = TOPIC_PATTERN.matcher(topic);
        if (!matcher.matches()) {
            throw new EventProcessingException(
                    "Topic must follow the format {namespace}/{entityType}/{entity}/{action}.");
        }

        if (!action.equals(matcher.group("action"))) {
            throw new EventProcessingException("Topic does not match event type.");
        }
        return matcher;
    }

    private Item getItem(String itemName) throws EventProcessingException {
        try {
            return itemRegistry.getItem(itemName);
        } catch (ItemNotFoundException e) {
            throw new EventProcessingException("Could not find item '" + itemName + "' in registry.");
        }
    }

    private Type parseType(@Nullable String payload) throws EventProcessingException {
        ItemEventPayloadBean bean = null;
        try {
            bean = gson.fromJson(payload, ItemEventPayloadBean.class);
        } catch (JsonParseException ignored) {
        }
        if (bean == null) {
            throw new EventProcessingException("Failed to deserialize payload '" + payload + "'.");
        }

        String simpleClassName = bean.type + TYPE_POSTFIX;
        Type returnType;

        if (simpleClassName.equals(UnDefType.class.getSimpleName())) {
            returnType = UnDefType.valueOf(bean.value);
        } else if (simpleClassName.equals(RefreshType.class.getSimpleName())) {
            returnType = RefreshType.valueOf(bean.value);
        } else {
            returnType = TypeParser.parseType(simpleClassName, bean.value);
        }

        if (returnType == null) {
            throw new EventProcessingException(
                    "Error parsing simpleClassName '" + simpleClassName + "' with value '" + bean.value + "'.");
        }

        return returnType;
    }

    private static class ItemEventPayloadBean {
        public @NonNullByDefault({}) String type;
        public @NonNullByDefault({}) String value;
    }
}
