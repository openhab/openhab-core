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
package org.openhab.core.voice.internal.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openhab.core.audio.AudioSink;
import org.openhab.core.audio.AudioSource;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.items.GroupItem;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.items.Metadata;
import org.openhab.core.items.MetadataKey;
import org.openhab.core.items.MetadataRegistry;
import org.openhab.core.items.events.ItemEventFactory;
import org.openhab.core.library.items.DimmerItem;
import org.openhab.core.library.items.StringItem;
import org.openhab.core.library.items.SwitchItem;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.types.CommandDescription;
import org.openhab.core.types.CommandOption;
import org.openhab.core.voice.DialogContext;
import org.openhab.core.voice.STTService;
import org.openhab.core.voice.TTSService;
import org.openhab.core.voice.text.InterpretationException;

/**
 * Test the standard interpreter
 *
 * @author Miguel Álvarez - Initial contribution
 */
@NonNullByDefault
@ExtendWith(MockitoExtension.class)
public class StandardInterpreterTest {

    private @Mock @NonNullByDefault({}) EventPublisher eventPublisherMock;

    private @Mock @NonNullByDefault({}) ItemRegistry itemRegistryMock;
    private @Mock @NonNullByDefault({}) MetadataRegistry metadataRegistryMock;
    private @NonNullByDefault({}) StandardInterpreter standardInterpreter;
    private @NonNullByDefault({}) STTService sttService;
    private @NonNullByDefault({}) TTSService ttsService;
    private @NonNullByDefault({}) AudioSource audioSource;
    private @NonNullByDefault({}) AudioSink audioSink;

    private static final String OK_RESPONSE = "Ok.";

    @BeforeEach
    public void setUp() {
        this.standardInterpreter = new StandardInterpreter(eventPublisherMock, itemRegistryMock, metadataRegistryMock);
    }

    @Test
    public void noNameCollisionOnSingleExactMatch() throws InterpretationException {
        var computerItem = new SwitchItem("computer");
        computerItem.setLabel("Computer");
        var computerScreenItem = new SwitchItem("screen");
        computerScreenItem.setLabel("Computer Screen");
        List<Item> items = List.of(computerItem, computerScreenItem);
        when(itemRegistryMock.getItems()).thenReturn(items);
        assertEquals(OK_RESPONSE, standardInterpreter.interpret(Locale.ENGLISH, "turn off computer"));
        verify(eventPublisherMock, times(1))
                .post(ItemEventFactory.createCommandEvent(computerItem.getName(), OnOffType.OFF));
    }

    @Test
    public void noNameCollisionOnSingleExactMatchForGroups() throws InterpretationException {
        var computerItem = Mockito.spy(new GroupItem("computer"));
        computerItem.setLabel("Computer");
        var computerSwitchItem = new SwitchItem("computer_power");
        computerSwitchItem.setLabel("Power");
        var screenItem = Mockito.spy(new GroupItem("screen"));
        screenItem.setLabel("Computer Screen");
        var screenSwitchItem = new SwitchItem("screen_power");
        screenSwitchItem.setLabel("Power");
        when(computerItem.getMembers()).thenReturn(Set.of(computerSwitchItem));
        when(screenItem.getMembers()).thenReturn(Set.of(screenSwitchItem));
        List<Item> items = List.of(computerItem, computerSwitchItem, screenItem, screenSwitchItem);
        when(itemRegistryMock.getItems()).thenReturn(items);
        assertEquals(OK_RESPONSE, standardInterpreter.interpret(Locale.ENGLISH, "turn off computer"));
        verify(eventPublisherMock, times(1))
                .post(ItemEventFactory.createCommandEvent(computerSwitchItem.getName(), OnOffType.OFF));
    }

    @Test
    public void noNameCollisionWhenDialogContext() throws InterpretationException {
        var locationItem = Mockito.spy(new GroupItem("livingroom"));
        locationItem.setLabel("Living room");
        var computerItem = new SwitchItem("computer");
        computerItem.setLabel("Computer");
        var computerItem2 = new SwitchItem("computer2");
        computerItem2.setLabel("Computer");
        when(locationItem.getMembers()).thenReturn(Set.of(computerItem));
        var dialogContext = new DialogContext(null, null, sttService, ttsService, null, List.of(), audioSource,
                audioSink, Locale.ENGLISH, "", locationItem.getName(), null, null);
        List<Item> items = List.of(computerItem2, locationItem, computerItem);
        when(itemRegistryMock.getItems()).thenReturn(items);
        assertEquals(OK_RESPONSE, standardInterpreter.interpret(Locale.ENGLISH, "turn off computer", dialogContext));
        verify(eventPublisherMock, times(1))
                .post(ItemEventFactory.createCommandEvent(computerItem.getName(), OnOffType.OFF));
    }

    @Test
    public void allowUseItemSynonyms() throws InterpretationException {
        var computerItem = new SwitchItem("computer");
        computerItem.setLabel("Computer");
        MetadataKey computerMetadataKey = new MetadataKey("synonyms", computerItem.getName());
        when(metadataRegistryMock.get(computerMetadataKey))
                .thenReturn(new Metadata(computerMetadataKey, "PC,Bedroom PC", null));
        when(metadataRegistryMock.get(new MetadataKey("voice-system", computerItem.getName()))).thenReturn(null);
        List<Item> items = List.of(computerItem);
        when(itemRegistryMock.getItems()).thenReturn(items);
        assertEquals(OK_RESPONSE, standardInterpreter.interpret(Locale.ENGLISH, "turn off computer"));
        verify(eventPublisherMock, times(1))
                .post(ItemEventFactory.createCommandEvent(computerItem.getName(), OnOffType.OFF));
        reset(eventPublisherMock);
        assertEquals(OK_RESPONSE, standardInterpreter.interpret(Locale.ENGLISH, "turn off pc"));
        verify(eventPublisherMock, times(1))
                .post(ItemEventFactory.createCommandEvent(computerItem.getName(), OnOffType.OFF));
        reset(eventPublisherMock);
        assertEquals(OK_RESPONSE, standardInterpreter.interpret(Locale.ENGLISH, "turn off bedroom pc"));
        verify(eventPublisherMock, times(1))
                .post(ItemEventFactory.createCommandEvent(computerItem.getName(), OnOffType.OFF));
    }

    @Test
    public void allowUseItemDescription() throws InterpretationException {
        var cmdDescription = new CommandDescription() {
            @Override
            public List<CommandOption> getCommandOptions() {
                return List.of(new CommandOption("10", "low"), new CommandOption("50", "medium"),
                        new CommandOption("90", "high"), new CommandOption("100", "high two"));
            }
        };
        var brightness = new DimmerItem("brightness") {
            @Override
            public @Nullable CommandDescription getCommandDescription() {
                return cmdDescription;
            }

            @Override
            public @Nullable CommandDescription getCommandDescription(@Nullable Locale locale) {
                return getCommandDescription();
            }
        };
        brightness.setLabel("Brightness");
        List<Item> items = List.of(brightness);
        when(itemRegistryMock.getItems()).thenReturn(items);
        assertEquals(OK_RESPONSE, standardInterpreter.interpret(Locale.ENGLISH, "set the brightness to low"));
        verify(eventPublisherMock, times(1))
                .post(ItemEventFactory.createCommandEvent(brightness.getName(), new PercentType(10)));
        reset(eventPublisherMock);
        assertEquals(OK_RESPONSE, standardInterpreter.interpret(Locale.ENGLISH, "set brightness to medium"));
        verify(eventPublisherMock, times(1))
                .post(ItemEventFactory.createCommandEvent(brightness.getName(), new PercentType(50)));
        reset(eventPublisherMock);
        assertEquals(OK_RESPONSE, standardInterpreter.interpret(Locale.ENGLISH, "set brightness high"));
        verify(eventPublisherMock, times(1))
                .post(ItemEventFactory.createCommandEvent(brightness.getName(), new PercentType(90)));
        reset(eventPublisherMock);
        assertEquals(OK_RESPONSE, standardInterpreter.interpret(Locale.ENGLISH, "set brightness high two"));
        verify(eventPublisherMock, times(1))
                .post(ItemEventFactory.createCommandEvent(brightness.getName(), new PercentType(100)));
    }

    @Test
    public void allowUseCustomCommands() throws InterpretationException {
        var virtualItem = new StringItem("virtual");
        MetadataKey voiceMetadataKey = new MetadataKey("voice-system", virtualItem.getName());
        when(metadataRegistryMock.get(voiceMetadataKey))
                .thenReturn(new Metadata(voiceMetadataKey, "watch|play * on|at? the? tv", null));
        List<Item> items = List.of(virtualItem);
        when(itemRegistryMock.getItems()).thenReturn(items);
        assertEquals(OK_RESPONSE, standardInterpreter.interpret(Locale.ENGLISH, "watch channel 4 on the tv"));
        verify(eventPublisherMock, times(1))
                .post(ItemEventFactory.createCommandEvent(virtualItem.getName(), new StringType("channel 4")));
        reset(eventPublisherMock);
    }

    @Test
    public void allowUseItemDescriptionOnCustomCommands() throws InterpretationException {
        var cmdDescription = new CommandDescription() {
            @Override
            public List<CommandOption> getCommandOptions() {
                return List.of(new CommandOption("KEY_4", "channel 4"));
            }
        };
        var virtualItem = new StringItem("virtual") {
            @Override
            public @Nullable CommandDescription getCommandDescription() {
                return cmdDescription;
            }

            @Override
            public @Nullable CommandDescription getCommandDescription(@Nullable Locale locale) {
                return getCommandDescription();
            }
        };
        MetadataKey voiceMetadataKey = new MetadataKey("voice-system", virtualItem.getName());
        when(metadataRegistryMock.get(voiceMetadataKey))
                .thenReturn(new Metadata(voiceMetadataKey, "watch|play * on|at? the? tv", null));
        List<Item> items = List.of(virtualItem);
        when(itemRegistryMock.getItems()).thenReturn(items);
        assertEquals(OK_RESPONSE, standardInterpreter.interpret(Locale.ENGLISH, "watch channel 4 on the tv"));
        verify(eventPublisherMock, times(1))
                .post(ItemEventFactory.createCommandEvent(virtualItem.getName(), new StringType("KEY_4")));
        reset(eventPublisherMock);
    }
}
