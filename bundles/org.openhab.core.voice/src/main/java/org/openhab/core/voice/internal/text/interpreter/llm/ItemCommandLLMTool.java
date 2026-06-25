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
package org.openhab.core.voice.internal.text.interpreter.llm;

import static org.openhab.core.voice.VoiceManager.VOICE_SOURCE;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemNotFoundException;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.items.events.ItemEventFactory;
import org.openhab.core.types.Command;
import org.openhab.core.types.TypeParser;
import org.openhab.core.voice.security.ItemPermission;
import org.openhab.core.voice.security.ItemPermissionResolver;
import org.openhab.core.voice.text.interpreter.llm.LLMTool;
import org.openhab.core.voice.text.interpreter.llm.LLMToolException;
import org.openhab.core.voice.text.interpreter.llm.LLMToolParam;
import org.openhab.core.voice.text.interpreter.llm.LLMToolParamType;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * The {@link ItemCommandLLMTool} is an {@link LLMTool} that allows to control items
 * by sending commands to them.
 *
 * @author Florian Hotze - Initial contribution
 */
@NonNullByDefault
@Component(service = LLMTool.class, immediate = true)
public class ItemCommandLLMTool implements LLMTool {
    public static final String ID = "item-send-command";

    private final ItemRegistry itemRegistry;
    private final ItemPermissionResolver itemPermissionResolver;
    private final EventPublisher eventPublisher;

    @Activate
    public ItemCommandLLMTool(final @Reference ItemRegistry itemRegistry,
            final @Reference ItemPermissionResolver itemPermissionResolver,
            final @Reference EventPublisher eventPublisher) {
        this.itemRegistry = itemRegistry;
        this.itemPermissionResolver = itemPermissionResolver;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public String getUID() {
        return ID;
    }

    @Override
    public String getLabel(@Nullable Locale locale) {
        return "Send Command to Item";
    }

    @Override
    public String getShortDescription(@Nullable Locale locale) {
        return "Sends a command to an Item.";
    }

    @Override
    public String getDescription(@Nullable Locale locale) {
        return """
                Control items via commands. Valid commands by item type:
                - Switch: ON, OFF
                - Dimmer: ON, OFF, Percent (0-100), INCREASE, DECREASE
                - Color: Dimmer commands, HSB (comma-separated Hue,Saturation,Brightness, e.g., '120,100,100')
                - Rollershutter: UP, DOWN, STOP, MOVE, Percent (0-100)
                - Player: PLAY, PAUSE, NEXT, PREVIOUS, REWIND, FASTFORWARD
                - Number: Decimals without (e.g., '21', '23.5') or with unit (e.g., '21 °C')
                - String: Text
                - DateTime: ISO 8601 (e.g., '2026-06-24T23:49:09+02:00')
                - Location: Latitude,longitude[,altitude] (e.g., '52.520008,13.404954')
                - Contact, Image, Call: REFRESH
                Items may accept command options (command/label pairs).
                """;
    }

    @Override
    public List<LLMToolParam> getParamDescriptions(@Nullable Locale locale) {
        return List.of(
                new LLMToolParam("itemName", LLMToolParamType.STRING, "The name of the item to control", List.of(),
                        true),
                new LLMToolParam("command", LLMToolParamType.STRING,
                        "The command to send. Must match the item type, e.g., ON/OFF for Switch/Dimmer, UP/DOWN/STOP/MOVE for Rollershutter, etc.",
                        List.of(), true));
    }

    @Override
    public String call(Map<String, Object> params, @Nullable Locale locale) throws LLMToolException {
        Object itemNameObj = params.get("itemName");
        Object commandObj = params.get("command");

        if (!(itemNameObj instanceof String itemName) || !(commandObj instanceof String commandString)) {
            throw new LLMToolException("Missing or invalid required parameters 'itemName' and 'command'");
        }

        Item item;
        try {
            item = itemRegistry.getItem(itemName);
        } catch (ItemNotFoundException e) {
            throw new LLMToolException("Item not found: " + itemName, e);
        }

        var permission = itemPermissionResolver.getPermission(item);
        if (permission == ItemPermission.NO_ACCESS) {
            throw new LLMToolException("Item not found: " + itemName);
        } else if (permission == ItemPermission.READ_ONLY) {
            throw new LLMToolException("Item is read-only: " + itemName);
        }

        Command command = TypeParser.parseCommand(item.getAcceptedCommandTypes(), commandString);
        if (command == null) {
            throw new LLMToolException("Failed to parse command '" + commandString + "' for item '" + itemName + "'");
        }

        eventPublisher.post(ItemEventFactory.createCommandEvent(itemName, command, VOICE_SOURCE));

        return "Successfully sent command '" + commandString + "' to item '" + itemName + "'.";
    }
}
