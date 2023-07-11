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

import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.items.GroupItem;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.items.Metadata;
import org.openhab.core.items.MetadataKey;
import org.openhab.core.items.MetadataRegistry;
import org.openhab.core.items.events.ItemEventFactory;
import org.openhab.core.library.items.SwitchItem;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.voice.text.InterpretationException;

/**
 * Test the standard interpreter
 *
 * @author Miguel Álvarez - Initial contribution
 */
@NonNullByDefault
public class StandardInterpreterTest {

    private @Mock @NonNullByDefault({}) EventPublisher eventPublisherMock;

    private @Mock @NonNullByDefault({}) ItemRegistry itemRegistryMock;
    private @Mock @NonNullByDefault({}) MetadataRegistry metadataRegistryMock;
    private @NonNullByDefault({}) StandardInterpreter standardInterpreter;
    private static final String OK_RESPONSE = "Ok.";

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        this.standardInterpreter = new StandardInterpreter(eventPublisherMock, itemRegistryMock, metadataRegistryMock);
    }

    @Test
    public void noNameCollisionOnSingleExactMatch() throws InterpretationException {
        var computerItem = new SwitchItem("computer");
        computerItem.setLabel("Computer");
        var computerScreenItem = new SwitchItem("screen");
        computerScreenItem.setLabel("Computer Screen");
        List<Item> items = List.of(computerItem, computerScreenItem);
        Mockito.when(itemRegistryMock.getAll()).thenReturn(items);
        Mockito.when(itemRegistryMock.getItems()).thenReturn(items);
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
        Mockito.when(computerItem.getMembers()).thenReturn(Set.of(computerSwitchItem));
        Mockito.when(screenItem.getMembers()).thenReturn(Set.of(screenSwitchItem));
        List<Item> items = List.of(computerItem, computerSwitchItem, screenItem, screenSwitchItem);
        Mockito.when(itemRegistryMock.getAll()).thenReturn(items);
        Mockito.when(itemRegistryMock.getItems()).thenReturn(items);
        assertEquals(OK_RESPONSE, standardInterpreter.interpret(Locale.ENGLISH, "turn off computer"));
        verify(eventPublisherMock, times(1))
                .post(ItemEventFactory.createCommandEvent(computerSwitchItem.getName(), OnOffType.OFF));
    }

    @Test
    public void allowUseItemSynonyms() throws InterpretationException {
        var computerItem = new SwitchItem("computer");
        computerItem.setLabel("Computer");
        MetadataKey computerMetadataKey = new MetadataKey("synonyms", computerItem.getName());
        Mockito.when(metadataRegistryMock.get(computerMetadataKey))
                .thenReturn(new Metadata(computerMetadataKey, "PC,Bedroom PC", null));
        List<Item> items = List.of(computerItem);
        Mockito.when(itemRegistryMock.getAll()).thenReturn(items);
        Mockito.when(itemRegistryMock.getItems()).thenReturn(items);
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
}
