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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.openhab.core.voice.internal.text.interpreter.StandardInterpreter.VOICE_SYSTEM_NAMESPACE;
import static org.openhab.core.voice.security.ItemPermissionResolver.PERMISSION_PROPERTY;
import static org.openhab.core.voice.text.interpreter.rulebased.AbstractRuleBasedInterpreter.IS_FORCED_CONFIGURATION;
import static org.openhab.core.voice.text.interpreter.rulebased.AbstractRuleBasedInterpreter.IS_SILENT_CONFIGURATION;
import static org.openhab.core.voice.text.interpreter.rulebased.AbstractRuleBasedInterpreter.IS_TEMPLATE_CONFIGURATION;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.AfterEach;
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
import org.openhab.core.library.CoreItemFactory;
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
import org.openhab.core.voice.internal.VoiceConfigurationConstants;
import org.openhab.core.voice.internal.security.ItemPermissionResolverImpl;
import org.openhab.core.voice.security.ItemPermission;
import org.openhab.core.voice.stt.STTService;
import org.openhab.core.voice.text.InterpretationException;
import org.openhab.core.voice.text.InterpreterContext;
import org.openhab.core.voice.text.conversation.Conversation;
import org.openhab.core.voice.text.conversation.ConversationException;
import org.openhab.core.voice.text.conversation.ConversationRole;
import org.openhab.core.voice.tts.TTSService;

/**
 * Test the standard interpreter
 *
 * @author Miguel Álvarez - Initial contribution
 * @author Florian Hotze - Implemented configurable Item access
 */
@NonNullByDefault
@ExtendWith(MockitoExtension.class)
public class StandardInterpreterTest {

    private @Mock @NonNullByDefault({}) EventPublisher eventPublisherMock;

    private @Mock @NonNullByDefault({}) ItemRegistry itemRegistryMock;
    private @Mock @NonNullByDefault({}) MetadataRegistry metadataRegistryMock;
    private @NonNullByDefault({}) ItemPermissionResolverImpl itemPermissionResolver;
    private @NonNullByDefault({}) StandardInterpreter standardInterpreter;
    private @NonNullByDefault({}) STTService sttService;
    private @NonNullByDefault({}) TTSService ttsService;
    private @NonNullByDefault({}) AudioSource audioSource;
    private @NonNullByDefault({}) AudioSink audioSink;

    private static final String OK_RESPONSE = "Ok.";

    @BeforeEach
    public void setUp() {
        itemPermissionResolver = new ItemPermissionResolverImpl(itemRegistryMock, metadataRegistryMock,
                Map.of(VoiceConfigurationConstants.CONFIG_IMPLICIT_ITEM_PERMISSION,
                        VoiceConfigurationConstants.DEFAULT_IMPLICIT_ITEM_ACCESS.name()));
        standardInterpreter = new StandardInterpreter(eventPublisherMock, itemRegistryMock, metadataRegistryMock,
                itemPermissionResolver);
    }

    @AfterEach
    public void tearDown() {
        itemPermissionResolver.dispose();
        itemPermissionResolver = null;
        standardInterpreter = null;
    }

    @Test
    public void noNameCollisionOnSingleExactMatch() throws InterpretationException {
        var computerItem = new SwitchItem("computer");
        computerItem.setLabel("Computer");
        var computerScreenItem = new SwitchItem("screen");
        computerScreenItem.setLabel("Computer Screen");
        List<Item> items = List.of(computerItem, computerScreenItem);
        when(itemRegistryMock.getAll()).thenReturn(items);

        // "computer" should only match computerItem, not computerScreenItem
        assertEquals(OK_RESPONSE, standardInterpreter.interpret(Locale.ENGLISH, "turn off computer"));
        verify(eventPublisherMock, times(1))
                .post(ItemEventFactory.createCommandEvent(computerItem.getName(), OnOffType.OFF, any()));
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
        when(itemRegistryMock.getAll()).thenReturn(items);

        // "computer" should only match the computerSwitchItem member of computerGroup
        assertEquals(OK_RESPONSE, standardInterpreter.interpret(Locale.ENGLISH, "turn off computer"));
        verify(eventPublisherMock, times(1))
                .post(ItemEventFactory.createCommandEvent(computerSwitchItem.getName(), OnOffType.OFF, any()));
    }

    @Test
    public void noNameCollisionWhenLocationItem() throws InterpretationException, ConversationException {
        var floor = new GroupItem("floor");
        floor.setLabel("ground floor");
        floor.addTag(CoreItemFactory.LOCATION);

        var kitchen = new GroupItem("kitchen");
        kitchen.setLabel("kitchen");
        kitchen.addTag(CoreItemFactory.LOCATION);
        kitchen.addGroupName("floor");
        floor.addMember(kitchen);

        var livingRoom = new GroupItem("livingRoom");
        livingRoom.setLabel("living room");
        livingRoom.addTag(CoreItemFactory.LOCATION);
        livingRoom.addGroupName("floor");
        floor.addMember(livingRoom);

        var light1 = new SwitchItem("light1");
        light1.setLabel("light");
        light1.addGroupName("kitchen");
        kitchen.addMember(light1);

        var light2 = new SwitchItem("light2");
        light2.setLabel("light");
        light2.addGroupName("livingRoom");
        livingRoom.addMember(light2);

        lenient().when(itemRegistryMock.getAll()).thenReturn(List.of(floor, kitchen, livingRoom, light1, light2));
        lenient().when(itemRegistryMock.get("floor")).thenReturn(floor);
        lenient().when(itemRegistryMock.get("kitchen")).thenReturn(kitchen);
        lenient().when(itemRegistryMock.get("livingRoom")).thenReturn(livingRoom);

        // Match light1 by location context kitchen
        Conversation conversation1 = new Conversation("c1");
        conversation1.addMessage(ConversationRole.USER, "turn on light");
        InterpreterContext context1 = new InterpreterContext(conversation1, List.of(), "kitchen", null);
        assertEquals(OK_RESPONSE, standardInterpreter.interpret(Locale.ENGLISH, context1));
        verify(eventPublisherMock, times(1)).post(ItemEventFactory.createCommandEvent("light1", OnOffType.ON, any()));

        reset(eventPublisherMock);
        // Match light2 by location context livingRoom
        Conversation conversation2 = new Conversation("c2");
        conversation2.addMessage(ConversationRole.USER, "turn on light");
        InterpreterContext context2 = new InterpreterContext(conversation2, List.of(), "livingRoom", null);
        assertEquals(OK_RESPONSE, standardInterpreter.interpret(Locale.ENGLISH, context2));
        verify(eventPublisherMock, times(1)).post(ItemEventFactory.createCommandEvent("light2", OnOffType.ON, any()));
    }

    @Test
    public void noNameCollisionOnSingleCommandTypeMatch() throws InterpretationException {
        // Both items have the same label "lamp"
        var switchItem = new SwitchItem("switch_lamp");
        switchItem.setLabel("lamp");
        var rollershutterItem = new RollershutterItem("rollershutter_lamp");
        rollershutterItem.setLabel("lamp");

        List<Item> items = List.of(switchItem, rollershutterItem);
        when(itemRegistryMock.getAll()).thenReturn(items);

        // "turn on" should only match the SwitchItem
        assertEquals(OK_RESPONSE, standardInterpreter.interpret(Locale.ENGLISH, "turn on the lamp"));
        verify(eventPublisherMock, times(1))
                .post(ItemEventFactory.createCommandEvent(switchItem.getName(), OnOffType.ON, any()));

        reset(eventPublisherMock);

        // "open" should only match the RollershutterItem
        assertEquals(OK_RESPONSE, standardInterpreter.interpret(Locale.ENGLISH, "open the lamp"));
        verify(eventPublisherMock, times(1))
                .post(ItemEventFactory.createCommandEvent(rollershutterItem.getName(), UpDownType.UP, any()));
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
        when(itemRegistryMock.getAll()).thenReturn(items);
        assertEquals(OK_RESPONSE, standardInterpreter.interpret(Locale.ENGLISH, "turn off computer"));
        verify(eventPublisherMock, times(1))
                .post(ItemEventFactory.createCommandEvent(computerItem.getName(), OnOffType.OFF, any()));
        reset(eventPublisherMock);
        assertEquals(OK_RESPONSE, standardInterpreter.interpret(Locale.ENGLISH, "turn off pc"));
        verify(eventPublisherMock, times(1))
                .post(ItemEventFactory.createCommandEvent(computerItem.getName(), OnOffType.OFF, any()));
        reset(eventPublisherMock);
        assertEquals(OK_RESPONSE, standardInterpreter.interpret(Locale.ENGLISH, "turn off bedroom pc"));
        verify(eventPublisherMock, times(1))
                .post(ItemEventFactory.createCommandEvent(computerItem.getName(), OnOffType.OFF, any()));
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
        when(itemRegistryMock.getAll()).thenReturn(items);
        assertEquals(OK_RESPONSE, standardInterpreter.interpret(Locale.ENGLISH, "set the brightness to low"));
        verify(eventPublisherMock, times(1))
                .post(ItemEventFactory.createCommandEvent(brightness.getName(), new PercentType(10), any()));
        reset(eventPublisherMock);
        assertEquals(OK_RESPONSE, standardInterpreter.interpret(Locale.ENGLISH, "set brightness to medium"));
        verify(eventPublisherMock, times(1))
                .post(ItemEventFactory.createCommandEvent(brightness.getName(), new PercentType(50), any()));
        reset(eventPublisherMock);
        assertEquals(OK_RESPONSE, standardInterpreter.interpret(Locale.ENGLISH, "set brightness high"));
        verify(eventPublisherMock, times(1))
                .post(ItemEventFactory.createCommandEvent(brightness.getName(), new PercentType(90), any()));
        reset(eventPublisherMock);
        assertEquals(OK_RESPONSE, standardInterpreter.interpret(Locale.ENGLISH, "set brightness high two"));
        verify(eventPublisherMock, times(1))
                .post(ItemEventFactory.createCommandEvent(brightness.getName(), new PercentType(100), any()));
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
        when(itemRegistryMock.getAll()).thenReturn(items);
        assertEquals(OK_RESPONSE, standardInterpreter.interpret(Locale.ENGLISH, "watch channel 4 on the tv"));
        verify(eventPublisherMock, times(1))
                .post(ItemEventFactory.createCommandEvent(tvItem.getName(), new StringType("KEY_4"), any()));
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
        when(itemRegistryMock.getAll()).thenReturn(items);
        assertEquals(OK_RESPONSE, standardInterpreter.interpret(Locale.ENGLISH, "watch channel 4 on the tv"));
        verify(eventPublisherMock, times(1))
                .post(ItemEventFactory.createCommandEvent(tvItem.getName(), new StringType("KEY_4"), any()));
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
        when(itemRegistryMock.getAll()).thenReturn(items);
        assertEquals(OK_RESPONSE, standardInterpreter.interpret(Locale.ENGLISH, "what time is it?"));
        verify(eventPublisherMock, times(1))
                .post(ItemEventFactory.createCommandEvent(triggerItem.getName(), new StringType("time"), any()));
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
        when(itemRegistryMock.getAll()).thenReturn(items);
        assertEquals(OK_RESPONSE, standardInterpreter.interpret(Locale.ENGLISH, "what time is it?"));
        verify(eventPublisherMock, times(1))
                .post(ItemEventFactory.createCommandEvent(triggerItem.getName(), new StringType("time"), any()));
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
        when(itemRegistryMock.getAll()).thenReturn(items);
        assertEquals(OK_RESPONSE, standardInterpreter.interpret(Locale.ENGLISH, "watch channel 4 on the tv"));
        verify(eventPublisherMock, times(1))
                .post(ItemEventFactory.createCommandEvent(tvItem.getName(), new StringType("channel 4"), any()));
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
        when(itemRegistryMock.getAll()).thenReturn(items);
        assertEquals("", standardInterpreter.interpret(Locale.ENGLISH, "watch channel 4 on the tv"));
        verify(eventPublisherMock, times(1))
                .post(ItemEventFactory.createCommandEvent(tvItem.getName(), new StringType("channel 4"), any()));
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
        when(itemRegistryMock.getAll()).thenReturn(items);
        assertEquals(OK_RESPONSE, standardInterpreter.interpret(Locale.ENGLISH, "watch channel 4 on the tv"));
        verify(eventPublisherMock, times(1))
                .post(ItemEventFactory.createCommandEvent(tvItem.getName(), new StringType("channel 4"), any()));
        reset(eventPublisherMock);
    }

    @Test
    public void allowUseCustomDynamicCommands() throws InterpretationException {
        var virtualItem = new StringItem("tv");
        MetadataKey voiceMetadataKey = new MetadataKey(VOICE_SYSTEM_NAMESPACE, virtualItem.getName());
        when(metadataRegistryMock.get(voiceMetadataKey))
                .thenReturn(new Metadata(voiceMetadataKey, "watch|play $*$", null));
        List<Item> items = List.of(virtualItem);
        when(itemRegistryMock.getAll()).thenReturn(items);
        assertEquals(OK_RESPONSE, standardInterpreter.interpret(Locale.ENGLISH, "watch channel 4"));
        verify(eventPublisherMock, times(1))
                .post(ItemEventFactory.createCommandEvent(virtualItem.getName(), new StringType("channel 4"), any()));
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
        when(itemRegistryMock.getAll()).thenReturn(items);
        assertEquals(OK_RESPONSE, standardInterpreter.interpret(Locale.ENGLISH, "watch channel 4 on the tv"));
        verify(eventPublisherMock, times(1))
                .post(ItemEventFactory.createCommandEvent(virtualItem.getName(), new StringType("KEY_4"), any()));
        reset(eventPublisherMock);
    }

    @Test
    public void openCloseBlindsTest() throws InterpretationException {
        var blindsItem = new RollershutterItem("blinds");
        blindsItem.setLabel("blinds");
        List<Item> items = List.of(blindsItem);
        when(itemRegistryMock.getAll()).thenReturn(items);

        assertEquals(OK_RESPONSE, standardInterpreter.interpret(Locale.ENGLISH, "open blinds"));
        verify(eventPublisherMock, times(1))
                .post(ItemEventFactory.createCommandEvent(blindsItem.getName(), UpDownType.UP, any()));

        reset(eventPublisherMock);
        assertEquals(OK_RESPONSE, standardInterpreter.interpret(Locale.ENGLISH, "open the blinds"));
        verify(eventPublisherMock, times(1))
                .post(ItemEventFactory.createCommandEvent(blindsItem.getName(), UpDownType.UP, any()));

        reset(eventPublisherMock);
        assertEquals(OK_RESPONSE, standardInterpreter.interpret(Locale.ENGLISH, "close blinds"));
        verify(eventPublisherMock, times(1))
                .post(ItemEventFactory.createCommandEvent(blindsItem.getName(), UpDownType.DOWN, any()));

        reset(eventPublisherMock);
        assertEquals(OK_RESPONSE, standardInterpreter.interpret(Locale.ENGLISH, "close the blinds"));
        verify(eventPublisherMock, times(1))
                .post(ItemEventFactory.createCommandEvent(blindsItem.getName(), UpDownType.DOWN, any()));
    }

    @Test
    public void denyAccessToItemViaMetadata() throws InterpretationException {
        var lightItem = new SwitchItem("light");
        lightItem.setLabel("Light");
        List<Item> items = List.of(lightItem);
        lenient().when(itemRegistryMock.getAll()).thenReturn(items);

        MetadataKey key = new MetadataKey(VOICE_SYSTEM_NAMESPACE, lightItem.getName());
        HashMap<String, Object> configuration = new HashMap<>();
        configuration.put(PERMISSION_PROPERTY, ItemPermission.NO_ACCESS.name());
        lenient().when(metadataRegistryMock.get(key)).thenReturn(new Metadata(key, "", configuration));

        // Should throw exception because item is not accessible
        InterpretationException exception = org.junit.jupiter.api.Assertions.assertThrows(InterpretationException.class,
                () -> {
                    standardInterpreter.interpret(Locale.ENGLISH, "turn on light");
                });
        assertEquals(noObjectsMessage(Locale.ENGLISH), exception.getMessage());
    }

    @Test
    public void allowAccessToItemViaMetadataWhenImplicitDenied() throws InterpretationException {
        itemPermissionResolver.modified(
                Map.of(VoiceConfigurationConstants.CONFIG_IMPLICIT_ITEM_PERMISSION, ItemPermission.NO_ACCESS.name()));

        var lightItem = new SwitchItem("light");
        lightItem.setLabel("Light");
        List<Item> items = List.of(lightItem);
        lenient().when(itemRegistryMock.getAll()).thenReturn(items);

        MetadataKey key = new MetadataKey(VOICE_SYSTEM_NAMESPACE, lightItem.getName());
        HashMap<String, Object> configuration = new HashMap<>();
        configuration.put(PERMISSION_PROPERTY, ItemPermission.READ_WRITE.name());
        lenient().when(metadataRegistryMock.get(key)).thenReturn(new Metadata(key, "", configuration));

        assertEquals(OK_RESPONSE, standardInterpreter.interpret(Locale.ENGLISH, "turn on light"));
        verify(eventPublisherMock, times(1)).post(ItemEventFactory.createCommandEvent("light", OnOffType.ON, any()));
    }

    @Test
    public void changingImplicitAccessInvalidatesCachedItemRules() throws InterpretationException {
        var lightItem = new SwitchItem("light");
        lightItem.setLabel("Light");
        lenient().when(itemRegistryMock.getAll()).thenReturn(List.of(lightItem));

        assertEquals(OK_RESPONSE, standardInterpreter.interpret(Locale.ENGLISH, "turn on light"));
        verify(eventPublisherMock, times(1)).post(ItemEventFactory.createCommandEvent("light", OnOffType.ON, any()));

        reset(eventPublisherMock);
        itemPermissionResolver.modified(
                Map.of(VoiceConfigurationConstants.CONFIG_IMPLICIT_ITEM_PERMISSION, ItemPermission.NO_ACCESS.name()));

        InterpretationException exception = org.junit.jupiter.api.Assertions.assertThrows(InterpretationException.class,
                () -> {
                    standardInterpreter.interpret(Locale.ENGLISH, "turn on light");
                });
        assertEquals(noObjectsMessage(Locale.ENGLISH), exception.getMessage());
    }

    @Test
    public void inheritAccessFromParentGroup() throws InterpretationException {
        var group = new GroupItem("allLights");
        var lightItem = new SwitchItem("light");
        lightItem.setLabel("Light");
        lightItem.addGroupName("allLights");

        lenient().when(itemRegistryMock.getAll()).thenReturn(List.of(group, lightItem));
        lenient().when(itemRegistryMock.get("allLights")).thenReturn(group);

        MetadataKey key = new MetadataKey(VOICE_SYSTEM_NAMESPACE, group.getName());
        HashMap<String, Object> configuration = new HashMap<>();
        configuration.put(PERMISSION_PROPERTY, ItemPermission.NO_ACCESS.name());
        lenient().when(metadataRegistryMock.get(key)).thenReturn(new Metadata(key, "", configuration));

        // Should throw exception because it inherits deny from group
        InterpretationException exception = org.junit.jupiter.api.Assertions.assertThrows(InterpretationException.class,
                () -> {
                    standardInterpreter.interpret(Locale.ENGLISH, "turn on light");
                });
        assertEquals(noObjectsMessage(Locale.ENGLISH), exception.getMessage());
    }

    @Test
    public void allowAccessToItemViaMetadataWhenParentGroupIsDenied() throws InterpretationException {
        var group = new GroupItem("allLights");
        group.setLabel("all lights");
        var lightItem = new SwitchItem("light");
        lightItem.setLabel("Light");
        lightItem.addGroupName("allLights");
        group.addMember(lightItem);

        lenient().when(itemRegistryMock.getAll()).thenReturn(List.of(group, lightItem));
        lenient().when(itemRegistryMock.get("allLights")).thenReturn(group);

        // Deny access to group
        MetadataKey groupKey = new MetadataKey(VOICE_SYSTEM_NAMESPACE, group.getName());
        HashMap<String, Object> groupConfig = new HashMap<>();
        groupConfig.put(PERMISSION_PROPERTY, ItemPermission.NO_ACCESS.name());
        lenient().when(metadataRegistryMock.get(groupKey)).thenReturn(new Metadata(groupKey, "", groupConfig));

        // Allow access to item explicitly
        MetadataKey itemKey = new MetadataKey(VOICE_SYSTEM_NAMESPACE, lightItem.getName());
        HashMap<String, Object> itemConfig = new HashMap<>();
        itemConfig.put(PERMISSION_PROPERTY, ItemPermission.READ_WRITE.name());
        lenient().when(metadataRegistryMock.get(itemKey)).thenReturn(new Metadata(itemKey, "", itemConfig));

        assertEquals(OK_RESPONSE, standardInterpreter.interpret(Locale.ENGLISH, "turn on light"));
        verify(eventPublisherMock, times(1)).post(ItemEventFactory.createCommandEvent("light", OnOffType.ON, any()));
    }

    @Test
    public void rejectCommandWhenReadOnly() throws InterpretationException {
        var lightItem = new SwitchItem("light");
        lightItem.setLabel("Light");
        List<Item> items = List.of(lightItem);
        lenient().when(itemRegistryMock.getAll()).thenReturn(items);

        MetadataKey key = new MetadataKey(VOICE_SYSTEM_NAMESPACE, lightItem.getName());
        HashMap<String, Object> configuration = new HashMap<>();
        configuration.put(PERMISSION_PROPERTY, ItemPermission.READ_ONLY.name());
        lenient().when(metadataRegistryMock.get(key)).thenReturn(new Metadata(key, "", configuration));

        assertEquals(readOnlyMessage(Locale.ENGLISH), standardInterpreter.interpret(Locale.ENGLISH, "turn on light"));
        verify(eventPublisherMock, never()).post(any());
    }

    private String noObjectsMessage(Locale locale) {
        return ResourceBundle.getBundle("LanguageSupport", locale).getString("no_objects");
    }

    private String readOnlyMessage(Locale locale) {
        return ResourceBundle.getBundle("LanguageSupport", locale).getString("read_only");
    }
}
