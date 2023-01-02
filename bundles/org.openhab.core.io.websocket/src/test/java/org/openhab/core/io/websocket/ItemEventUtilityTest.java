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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openhab.core.events.Event;
import org.openhab.core.items.ItemNotFoundException;
import org.openhab.core.items.ItemRegistry;
import org.openhab.core.items.events.ItemEvent;
import org.openhab.core.items.events.ItemEventFactory;
import org.openhab.core.library.items.StringItem;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.HSBType;
import org.openhab.core.library.types.StringType;

import com.google.gson.Gson;

/**
 * The {@link ItemEventUtilityTest} contains tests for the {@link ItemEventUtility} class.
 *
 * @author Jan N. Klug - Initial contribution
 */
@NonNullByDefault
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ItemEventUtilityTest {
    private static final String EXISTING_ITEM_NAME = "existingItem";
    private static final String NON_EXISTING_ITEM_NAME = "nonExistingItem";
    private static final StringType ITEM_STATE = new StringType("foo");

    private @Mock @NonNullByDefault({}) ItemRegistry itemRegistry;

    private StringItem existingItem = new StringItem(EXISTING_ITEM_NAME);
    private Gson gson = new Gson();

    private @NonNullByDefault({}) ItemEventUtility itemEventUtility;

    @BeforeEach
    public void setUp() throws ItemNotFoundException {
        itemEventUtility = new ItemEventUtility(gson, itemRegistry);

        when(itemRegistry.getItem(eq(EXISTING_ITEM_NAME))).thenReturn(existingItem);
        when(itemRegistry.getItem(eq(NON_EXISTING_ITEM_NAME)))
                .thenThrow(new ItemNotFoundException(NON_EXISTING_ITEM_NAME));
    }

    @Test
    public void validStateEvent() throws EventProcessingException {
        ItemEvent event = ItemEventFactory.createStateEvent(EXISTING_ITEM_NAME, ITEM_STATE);
        EventDTO eventDTO = new EventDTO(event);

        Event itemEvent = itemEventUtility.createStateEvent(eventDTO);

        assertThat(itemEvent, is(event));
    }

    @Test
    public void validStateEventWithMissingItem() {
        ItemEvent event = ItemEventFactory.createStateEvent(NON_EXISTING_ITEM_NAME, ITEM_STATE);
        EventDTO eventDTO = new EventDTO(event);

        EventProcessingException e = assertThrows(EventProcessingException.class,
                () -> itemEventUtility.createStateEvent(eventDTO));
        assertThat(e.getMessage(), is("Could not find item '" + NON_EXISTING_ITEM_NAME + "' in registry."));
    }

    @Test
    public void validStateEventWithInvalidState() {
        ItemEvent event = ItemEventFactory.createStateEvent(EXISTING_ITEM_NAME, DecimalType.ZERO);
        EventDTO eventDTO = new EventDTO(event);

        EventProcessingException e = assertThrows(EventProcessingException.class,
                () -> itemEventUtility.createStateEvent(eventDTO));
        assertThat(e.getMessage(), is("Incompatible datatype, rejected."));
    }

    @Test
    public void invalidStateEventTopic() {
        ItemEvent event = ItemEventFactory.createCommandEvent(EXISTING_ITEM_NAME, HSBType.BLACK);
        EventDTO eventDTO = new EventDTO(event);

        EventProcessingException e = assertThrows(EventProcessingException.class,
                () -> itemEventUtility.createStateEvent(eventDTO));
        assertThat(e.getMessage(), is("Topic does not match event type."));
    }

    @Test
    public void invalidStateEventPayload() {
        ItemEvent event = ItemEventFactory.createStateEvent(EXISTING_ITEM_NAME, HSBType.BLACK);
        EventDTO eventDTO = new EventDTO(event);
        eventDTO.payload = "invalidNoJson";

        EventProcessingException e = assertThrows(EventProcessingException.class,
                () -> itemEventUtility.createStateEvent(eventDTO));
        assertThat(e.getMessage(), is("Failed to deserialize payload 'invalidNoJson'."));
    }

    @Test
    public void validCommandEvent() throws EventProcessingException {
        ItemEvent event = ItemEventFactory.createCommandEvent(EXISTING_ITEM_NAME, ITEM_STATE);
        EventDTO eventDTO = new EventDTO(event);

        Event itemEvent = itemEventUtility.createCommandEvent(eventDTO);

        assertThat(itemEvent, is(event));
    }

    @Test
    public void validCommandEventWithMissingItem() {
        ItemEvent event = ItemEventFactory.createStateEvent(NON_EXISTING_ITEM_NAME, ITEM_STATE);
        EventDTO eventDTO = new EventDTO(event);

        EventProcessingException e = assertThrows(EventProcessingException.class,
                () -> itemEventUtility.createStateEvent(eventDTO));
        assertThat(e.getMessage(), is("Could not find item '" + NON_EXISTING_ITEM_NAME + "' in registry."));
    }

    @Test
    public void validCommandEventWithInvalidState() {
        ItemEvent event = ItemEventFactory.createCommandEvent(EXISTING_ITEM_NAME, HSBType.BLACK);
        EventDTO eventDTO = new EventDTO(event);

        EventProcessingException e = assertThrows(EventProcessingException.class,
                () -> itemEventUtility.createCommandEvent(eventDTO));
        assertThat(e.getMessage(), is("Incompatible datatype, rejected."));
    }

    @Test
    public void invalidCommandEvent() {
        ItemEvent event = ItemEventFactory.createStateEvent(EXISTING_ITEM_NAME, HSBType.BLACK);
        EventDTO eventDTO = new EventDTO(event);

        EventProcessingException e = assertThrows(EventProcessingException.class,
                () -> itemEventUtility.createCommandEvent(eventDTO));
        assertThat(e.getMessage(), is("Topic does not match event type."));
    }

    @Test
    public void invalidCommandEventPayload() {
        ItemEvent event = ItemEventFactory.createCommandEvent(EXISTING_ITEM_NAME, HSBType.BLACK);
        EventDTO eventDTO = new EventDTO(event);
        eventDTO.payload = "invalidNoJson";

        EventProcessingException e = assertThrows(EventProcessingException.class,
                () -> itemEventUtility.createCommandEvent(eventDTO));
        assertThat(e.getMessage(), is("Failed to deserialize payload 'invalidNoJson'."));
    }
}
