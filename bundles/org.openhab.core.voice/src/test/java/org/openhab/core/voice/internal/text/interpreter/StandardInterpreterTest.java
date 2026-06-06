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
package org.openhab.core.voice.internal.text.interpreter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.openhab.core.voice.internal.text.interpreter.StandardInterpreter.VOICE_SYSTEM_NAMESPACE;
import static org.openhab.core.voice.text.interpreter.rulebased.AbstractRuleBasedInterpreter.IS_FORCED_CONFIGURATION;
import static org.openhab.core.voice.text.interpreter.rulebased.AbstractRuleBasedInterpreter.IS_SILENT_CONFIGURATION;
import static org.openhab.core.voice.text.interpreter.rulebased.AbstractRuleBasedInterpreter.IS_TEMPLATE_CONFIGURATION;

import java.util.Collections;
import java.util.HashMap;
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
import org.openhab.core.library.items.RollershutterItem;
import org.openhab.core.library.items.StringItem;
import org.openhab.core.library.items.SwitchItem;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.library.types.UpDownType;
import org.openhab.core.types.CommandDescription;
import org.openhab.core.types.CommandOption;
import org.openhab.core.types.State;
import org.openhab.core.voice.STTService;
import org.openhab.core.voice.TTSService;
import org.openhab.core.voice.text.InterpretationException;
import org.openhab.core.voice.text.InterpreterContext;
import org.openhab.core.voice.text.conversation.Conversation;
import org.openhab.core.voice.text.conversation.ConversationException;
import org.openhab.core.voice.text.conversation.ConversationRole;

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

        // "computer" should only match computerItem, not computerScreenItem
        assertEquals(OK_RESPONSE, standardInterpreter.interpret(Locale.ENGLISH, "turn off computer"));
        verify(eventPublisherMock, times(1))
                .post(ItemEventFactory.createCommandEvent(computerItem.getName(), OnOffType.OFF));
    }

    @Test
    public void noNameCollisionOnSingleExactMatchForGroups() throws InterpretationException {
        var computerGroup = Mockito.spy(new GroupItem("computer"));
        computerGroup.setLabel("Computer");
        var computerSwitchItem = new SwitchItem("computer_power");
        computerSwitchItem.setLabel("Power");
        var screenGroup = Mockito.spy(new GroupItem("screen"));
        screenGroup.setLabel("Computer Screen");
        var screenSwitchItem = new SwitchItem("screen_power");
        screenSwitchItem.setLabel("Power");
        when(computerGroup.getMembers()).thenReturn(Set.of(computerSwitchItem));
        when(screenGroup.getMembers()).thenReturn(Set.of(screenSwitchItem));
        List<Item> items = List.of(computerGroup, computerSwitchItem, screenGroup, screenSwitchItem);
        when(itemRegistryMock.getItems()).thenReturn(items);

        // "computer" should only match the computerSwitchItem member of computerGroup
        assertEquals(OK_RESPONSE, standardInterpreter.interpret(Locale.ENGLISH, "turn off computer"));
        verify(eventPublisherMock, times(1))
                .post(ItemEventFactory.createCommandEvent(computerSwitchItem.getName(), OnOffType.OFF));
    }

    @Test
    public void noNameCollisionWhenLocationItem() throws InterpretationException, ConversationException {
        var locationGroup = Mockito.spy(new GroupItem("livingroom"));
        locationGroup.setLabel("Living room");
        var computerItem = new SwitchItem("computer");
        computerItem.setLabel("Computer");
        var computerItem2 = new SwitchItem("computer2");
        computerItem2.setLabel("Computer");
        when(locationGroup.getMembers()).thenReturn(Set.of(computerItem));
        List<Item> items = List.of(computerItem2, locationGroup, computerItem);
        when(itemRegistryMock.getItems()).thenReturn(items);
        Conversation conversation = new Conversation("test-conversation");
        conversation.addMessage(ConversationRole.USER, "turn off computer");
        InterpreterContext interpreterContext = new InterpreterContext(conversation, Collections.emptyList(),
                locationGroup.getName());

        // "computer" should only match the computerItem in the locationGroup
        assertEquals(OK_RESPONSE, standardInterpreter.interpret(Locale.ENGLISH, interpreterContext));
        verify(eventPublisherMock, times(1))
                .post(ItemEventFactory.createCommandEvent(computerItem.getName(), OnOffType.OFF));
    }

    @Test
    public void noNameCollisionOnSingleCommandTypeMatch() throws InterpretationException {
        // Both items have the same label "lamp"
        var switchItem = new SwitchItem("switch_lamp");
        switchItem.setLabel("lamp");
        var rollershutterItem = new RollershutterItem("rollershutter_lamp");
        rollershutterItem.setLabel("lamp");

        List<Item> items = List.of(switchItem, rollershutterItem);
        when(itemRegistryMock.getItems()).thenReturn(items);

        // "turn on" should only match the SwitchItem
        assertEquals(OK_RESPONSE, standardInterpreter.interpret(Locale.ENGLISH, "turn on the lamp"));
        verify(eventPublisherMock, times(1))
                .post(ItemEventFactory.createCommandEvent(switchItem.getName(), OnOffType.ON));

        reset(eventPublisherMock);

        // "open" should only match the RollershutterItem
        assertEquals(OK_RESPONSE, standardInterpreter.interpret(Locale.ENGLISH, "open the lamp"));
        verify(eventPublisherMock, times(1))
                .post(ItemEventFactory.createCommandEvent(rollershutterItem.getName(), UpDownType.UP));
    }

    @Test
    public void allowUseItemSynonyms() throws InterpretationException {
        var computerItem = new SwitchItem("computer");
        computerItem.setLabel("Computer");
        MetadataKey computerMetadataKey = new MetadataKey("synonyms", computerItem.getName());
        when(metadataRegistryMock.get(computerMetadataKey))
                .thenReturn(new Metadata(computerMetadataKey, "PC,Bedroom PC", null));
        when(metadataRegistryMock.get(new MetadataKey(VOICE_SYSTEM_NAMESPACE, computerItem.getName())))
                .thenReturn(null);
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
        var brightness = new DimmerItem("brightness") {
            @Override
            public @Nullable CommandDescription getCommandDescription() {
                return () -> List.of(new CommandOption("10", "low"), new CommandOption("50", "medium"),
                        new CommandOption("90", "high"), new CommandOption("100", "high two"));
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
    public void allowUseCustomItemCommands() throws InterpretationException {
        var tvItem = new StringItem("virtual") {
            @Override
            public @Nullable CommandDescription getCommandDescription() {
                return () -> List.of(new CommandOption("KEY_4", "channel 4"));
            }

            @Override
            public @Nullable CommandDescription getCommandDescription(@Nullable Locale locale) {
                return getCommandDescription();
            }
        };
        tvItem.setLabel("tv");
        MetadataKey voiceMetadataKey = new MetadataKey(VOICE_SYSTEM_NAMESPACE, tvItem.getName());
        when(metadataRegistryMock.get(voiceMetadataKey))
                .thenReturn(new Metadata(voiceMetadataKey, "watch|play $cmd$ on|at the? $name$", null));
        List<Item> items = List.of(tvItem);
        when(itemRegistryMock.getItems()).thenReturn(items);
        assertEquals(OK_RESPONSE, standardInterpreter.interpret(Locale.ENGLISH, "watch channel 4 on the tv"));
        verify(eventPublisherMock, times(1))
                .post(ItemEventFactory.createCommandEvent(tvItem.getName(), new StringType("KEY_4")));
        reset(eventPublisherMock);
    }

    @Test
    public void allowUseCustomCommandCommands() throws InterpretationException {
        var tvItem = new StringItem("tv") {
            @Override
            public @Nullable CommandDescription getCommandDescription() {
                return () -> List.of(new CommandOption("KEY_4", "channel 4"));
            }

            @Override
            public @Nullable CommandDescription getCommandDescription(@Nullable Locale locale) {
                return getCommandDescription();
            }
        };
        MetadataKey voiceMetadataKey = new MetadataKey(VOICE_SYSTEM_NAMESPACE, tvItem.getName());
        when(metadataRegistryMock.get(voiceMetadataKey))
                .thenReturn(new Metadata(voiceMetadataKey, "watch|play $cmd$ on|at the? tv", null));
        List<Item> items = List.of(tvItem);
        when(itemRegistryMock.getItems()).thenReturn(items);
        assertEquals(OK_RESPONSE, standardInterpreter.interpret(Locale.ENGLISH, "watch channel 4 on the tv"));
        verify(eventPublisherMock, times(1))
                .post(ItemEventFactory.createCommandEvent(tvItem.getName(), new StringType("KEY_4")));
        reset(eventPublisherMock);
    }

    @Test
    public void allowHandleQuestionWithCustomCommand() throws InterpretationException {
        var triggerItem = new StringItem("trigger_item") {
            @Override
            public @Nullable CommandDescription getCommandDescription() {
                return () -> List.of(new CommandOption("day", "day"), new CommandOption("time", "time"));
            }

            @Override
            public @Nullable CommandDescription getCommandDescription(@Nullable Locale locale) {
                return getCommandDescription();
            }
        };
        MetadataKey voiceMetadataKey = new MetadataKey(VOICE_SYSTEM_NAMESPACE, triggerItem.getName());
        when(metadataRegistryMock.get(voiceMetadataKey))
                .thenReturn(new Metadata(voiceMetadataKey, "what $cmd$ is it", null));
        List<Item> items = List.of(triggerItem);
        when(itemRegistryMock.getItems()).thenReturn(items);
        assertEquals(OK_RESPONSE, standardInterpreter.interpret(Locale.ENGLISH, "what time is it?"));
        verify(eventPublisherMock, times(1))
                .post(ItemEventFactory.createCommandEvent(triggerItem.getName(), new StringType("time")));
        reset(eventPublisherMock);
    }

    @Test
    public void allowForceCustomCommand() throws InterpretationException {
        var triggerItem = new StringItem("trigger_item") {
            @Override
            public @Nullable CommandDescription getCommandDescription() {
                return () -> List.of(new CommandOption("day", "day"), new CommandOption("time", "time"));
            }

            @Override
            public @Nullable CommandDescription getCommandDescription(@Nullable Locale locale) {
                return getCommandDescription();
            }

            @Override
            public <T extends State> @Nullable T getStateAs(Class<T> typeClass) {
                return (T) new StringType("time");
            }
        };
        HashMap<String, Object> configuration = new HashMap<>();
        configuration.put(IS_FORCED_CONFIGURATION, true);
        MetadataKey voiceMetadataKey = new MetadataKey(VOICE_SYSTEM_NAMESPACE, triggerItem.getName());
        when(metadataRegistryMock.get(voiceMetadataKey))
                .thenReturn(new Metadata(voiceMetadataKey, "what $cmd$ is it", configuration));
        List<Item> items = List.of(triggerItem);
        when(itemRegistryMock.getItems()).thenReturn(items);
        assertEquals(OK_RESPONSE, standardInterpreter.interpret(Locale.ENGLISH, "what time is it?"));
        verify(eventPublisherMock, times(1))
                .post(ItemEventFactory.createCommandEvent(triggerItem.getName(), new StringType("time")));
        reset(eventPublisherMock);
    }

    @Test
    public void allowUseCustomItemDynamicCommands() throws InterpretationException {
        var tvItem = new StringItem("tv");
        tvItem.setLabel("tv");
        MetadataKey voiceMetadataKey = new MetadataKey(VOICE_SYSTEM_NAMESPACE, tvItem.getName());
        when(metadataRegistryMock.get(voiceMetadataKey))
                .thenReturn(new Metadata(voiceMetadataKey, "watch|play $*$ on|at the? $name$", null));
        List<Item> items = List.of(tvItem);
        when(itemRegistryMock.getItems()).thenReturn(items);
        assertEquals(OK_RESPONSE, standardInterpreter.interpret(Locale.ENGLISH, "watch channel 4 on the tv"));
        verify(eventPublisherMock, times(1))
                .post(ItemEventFactory.createCommandEvent(tvItem.getName(), new StringType("channel 4")));
        reset(eventPublisherMock);
    }

    @Test
    public void allowUseCommandsWithoutAnswer() throws InterpretationException {
        var tvItem = new StringItem("tv");
        tvItem.setLabel("tv");
        MetadataKey voiceMetadataKey = new MetadataKey(VOICE_SYSTEM_NAMESPACE, tvItem.getName());
        HashMap<String, Object> configuration = new HashMap<>();
        configuration.put(IS_SILENT_CONFIGURATION, true);
        when(metadataRegistryMock.get(voiceMetadataKey))
                .thenReturn(new Metadata(voiceMetadataKey, "watch|play $*$ on|at the? $name$", configuration));
        List<Item> items = List.of(tvItem);
        when(itemRegistryMock.getItems()).thenReturn(items);
        assertEquals("", standardInterpreter.interpret(Locale.ENGLISH, "watch channel 4 on the tv"));
        verify(eventPublisherMock, times(1))
                .post(ItemEventFactory.createCommandEvent(tvItem.getName(), new StringType("channel 4")));
        reset(eventPublisherMock);
    }

    @Test
    public void allowUseCommandsFromTemplate() throws InterpretationException {
        var virtualItem = new StringItem("virtual");
        virtualItem.setLabel("tv rule");
        virtualItem.addTag("tv");
        var tvItem = new StringItem("tv");
        tvItem.setLabel("tv");
        tvItem.addTag("tv");
        MetadataKey voiceMetadataKey = new MetadataKey(VOICE_SYSTEM_NAMESPACE, virtualItem.getName());
        HashMap<String, Object> configuration = new HashMap<>();
        configuration.put(IS_TEMPLATE_CONFIGURATION, true);
        when(metadataRegistryMock.get(voiceMetadataKey))
                .thenReturn(new Metadata(voiceMetadataKey, "watch|play $*$ on|at the? $name$", configuration));
        List<Item> items = List.of(virtualItem, tvItem);
        when(itemRegistryMock.getItems()).thenReturn(items);
        assertEquals(OK_RESPONSE, standardInterpreter.interpret(Locale.ENGLISH, "watch channel 4 on the tv"));
        verify(eventPublisherMock, times(1))
                .post(ItemEventFactory.createCommandEvent(tvItem.getName(), new StringType("channel 4")));
        reset(eventPublisherMock);
    }

    @Test
    public void allowUseCustomDynamicCommands() throws InterpretationException {
        var virtualItem = new StringItem("tv");
        MetadataKey voiceMetadataKey = new MetadataKey(VOICE_SYSTEM_NAMESPACE, virtualItem.getName());
        when(metadataRegistryMock.get(voiceMetadataKey))
                .thenReturn(new Metadata(voiceMetadataKey, "watch|play $*$", null));
        List<Item> items = List.of(virtualItem);
        when(itemRegistryMock.getItems()).thenReturn(items);
        assertEquals(OK_RESPONSE, standardInterpreter.interpret(Locale.ENGLISH, "watch channel 4"));
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
        MetadataKey voiceMetadataKey = new MetadataKey(VOICE_SYSTEM_NAMESPACE, virtualItem.getName());
        when(metadataRegistryMock.get(voiceMetadataKey))
                .thenReturn(new Metadata(voiceMetadataKey, "watch|play $*$ on|at? the? tv", null));
        List<Item> items = List.of(virtualItem);
        when(itemRegistryMock.getItems()).thenReturn(items);
        assertEquals(OK_RESPONSE, standardInterpreter.interpret(Locale.ENGLISH, "watch channel 4 on the tv"));
        verify(eventPublisherMock, times(1))
                .post(ItemEventFactory.createCommandEvent(virtualItem.getName(), new StringType("KEY_4")));
        reset(eventPublisherMock);
    }

    @Test
    public void openCloseBlindsTest() throws InterpretationException {
        var blindsItem = new RollershutterItem("blinds");
        blindsItem.setLabel("blinds");
        List<Item> items = List.of(blindsItem);
        when(itemRegistryMock.getItems()).thenReturn(items);

        assertEquals(OK_RESPONSE, standardInterpreter.interpret(Locale.ENGLISH, "open blinds"));
        verify(eventPublisherMock, times(1))
                .post(ItemEventFactory.createCommandEvent(blindsItem.getName(), UpDownType.UP));

        reset(eventPublisherMock);
        assertEquals(OK_RESPONSE, standardInterpreter.interpret(Locale.ENGLISH, "open the blinds"));
        verify(eventPublisherMock, times(1))
                .post(ItemEventFactory.createCommandEvent(blindsItem.getName(), UpDownType.UP));

        reset(eventPublisherMock);
        assertEquals(OK_RESPONSE, standardInterpreter.interpret(Locale.ENGLISH, "close blinds"));
        verify(eventPublisherMock, times(1))
                .post(ItemEventFactory.createCommandEvent(blindsItem.getName(), UpDownType.DOWN));

        reset(eventPublisherMock);
        assertEquals(OK_RESPONSE, standardInterpreter.interpret(Locale.ENGLISH, "close the blinds"));
        verify(eventPublisherMock, times(1))
                .post(ItemEventFactory.createCommandEvent(blindsItem.getName(), UpDownType.DOWN));
    }
}
