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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.items.Item;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.items.Metadata;
import org.openhab.core.items.MetadataKey;
import org.openhab.core.items.MetadataRegistry;
import org.openhab.core.library.items.SwitchItem;
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

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        List<Item> items = new ArrayList<>();
        var computerItem = new SwitchItem("computer");
        computerItem.setLabel("Computer");
        MetadataKey computerMetadataKey = new MetadataKey("synonyms", computerItem.getName());
        Mockito.when(metadataRegistryMock.get(computerMetadataKey))
                .thenReturn(new Metadata(computerMetadataKey, "PC,Bedroom PC", null));
        var computerScreenItem = new SwitchItem("screen");
        computerScreenItem.setLabel("Computer Screen");
        items.add(computerItem);
        items.add(computerScreenItem);
        Mockito.when(itemRegistryMock.getAll()).thenReturn(items);
        Mockito.when(itemRegistryMock.getItems()).thenReturn(items);
        this.standardInterpreter = new StandardInterpreter(eventPublisherMock, itemRegistryMock, metadataRegistryMock);
    }

    @Test
    public void noNameCollisionOnSingleExactMatch() throws InterpretationException {
        assertEquals("Ok.", standardInterpreter.interpret(Locale.ENGLISH, "turn off computer"));
    }

    @Test
    public void allowUseItemSynonyms() throws InterpretationException {
        assertEquals("Ok.", standardInterpreter.interpret(Locale.ENGLISH, "turn off pc"));
        assertEquals("Ok.", standardInterpreter.interpret(Locale.ENGLISH, "turn off bedroom pc"));
    }
}
