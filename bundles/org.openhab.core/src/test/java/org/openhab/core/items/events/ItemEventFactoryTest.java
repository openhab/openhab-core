/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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
package org.openhab.core.items.events;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

import org.junit.Test;
import org.openhab.core.events.Event;
import org.openhab.core.items.Item;
import org.openhab.core.items.dto.ItemDTOMapper;
import org.openhab.core.library.CoreItemFactory;
import org.openhab.core.library.items.SwitchItem;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.RawType;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.openhab.core.types.State;
import org.openhab.core.types.UnDefType;

import com.google.gson.Gson;

/**
 * {@link ItemEventFactoryTests} tests the {@link ItemEventFactory}.
 *
 * @author Stefan Bußweiler - Initial contribution
 */
public class ItemEventFactoryTest {
    private final ItemEventFactory factory = new ItemEventFactory();

    private static final String ITEM_NAME = "ItemA";
    private static final Item ITEM = new SwitchItem(ITEM_NAME);
    private static final String GROUP_NAME = "GroupA";
    private static final String SOURCE = "binding:type:id:channel";

    private static final String ITEM_COMMAND_EVENT_TYPE = ItemCommandEvent.TYPE;
    private static final String ITEM_STATE_EVENT_TYPE = ItemStateEvent.TYPE;
    private static final String ITEM_STATE_PREDICTED_EVENT_TYPE = ItemStatePredictedEvent.TYPE;
    private static final String ITEM_ADDED_EVENT_TYPE = ItemAddedEvent.TYPE;
    private static final String GROUPITEM_CHANGED_EVENT_TYPE = GroupItemStateChangedEvent.TYPE;

    private static final String ITEM_COMMAND_EVENT_TOPIC = "smarthome/items/" + ITEM_NAME + "/command";
    private static final String ITEM_STATE_EVENT_TOPIC = "smarthome/items/" + ITEM_NAME + "/state";
    private static final String ITEM_STATE_PREDICTED_EVENT_TOPIC = "smarthome/items/" + ITEM_NAME + "/statepredicted";
    private static final String ITEM_ADDED_EVENT_TOPIC = "smarthome/items/" + ITEM_NAME + "/added";
    private static final String GROUPITEM_STATE_CHANGED_EVENT_TOPIC = "smarthome/items/" + GROUP_NAME + "/" + ITEM_NAME
            + "/statechanged";

    private static final Command ITEM_COMMAND = OnOffType.ON;
    private static final String ITEM_COMMAND_EVENT_PAYLOAD = "{\"type\":\"OnOff\",\"value\":\"ON\"}";
    private static final String ITEM_REFRESH_COMMAND_EVENT_PAYLOAD = "{\"type\":\"Refresh\", \"value\": \"REFRESH\"}";
    private static final String ITEM_UNDEF_STATE_EVENT_PAYLOAD = "{\"type\":\"UnDef\", \"value\": \"UNDEF\"}";
    private static final State ITEM_STATE = OnOffType.OFF;
    private static final State NEW_ITEM_STATE = OnOffType.ON;
    private static final String ITEM_STATE_EVENT_PAYLOAD = "{\"type\":\"OnOff\",\"value\":\"OFF\"}";
    private static final String ITEM_STATE_PREDICTED_EVENT_PAYLOAD = "{\"predictedType\":\"OnOff\",\"predictedValue\":\"OFF\",\"isConfirmation\":\"false\"}";
    private static final String ITEM_ADDED_EVENT_PAYLOAD = new Gson().toJson(ItemDTOMapper.map(ITEM));
    private static final String ITEM_STATE_CHANGED_EVENT_PAYLOAD = "{\"type\":\"OnOff\", \"value\": \"ON\", \"oldType\":\"OnOff\", \"oldValue\": \"OFF\"}";

    private static final State RAW_ITEM_STATE = new RawType(new byte[] { 1, 2, 3, 4, 5 }, RawType.DEFAULT_MIME_TYPE);
    private static final State NEW_RAW_ITEM_STATE = new RawType(new byte[] { 5, 4, 3, 2, 1 },
            RawType.DEFAULT_MIME_TYPE);

    @Test
    public void testCreateEventItemCommandEventOnOffType() throws Exception {
        Event event = factory.createEvent(ITEM_COMMAND_EVENT_TYPE, ITEM_COMMAND_EVENT_TOPIC, ITEM_COMMAND_EVENT_PAYLOAD,
                SOURCE);

        assertEquals(ItemCommandEvent.class, event.getClass());
        ItemCommandEvent itemCommandEvent = (ItemCommandEvent) event;
        assertEquals(ITEM_COMMAND_EVENT_TYPE, itemCommandEvent.getType());
        assertEquals(ITEM_COMMAND_EVENT_TOPIC, itemCommandEvent.getTopic());
        assertEquals(ITEM_COMMAND_EVENT_PAYLOAD, itemCommandEvent.getPayload());
        assertEquals(ITEM_NAME, itemCommandEvent.getItemName());
        assertEquals(SOURCE, itemCommandEvent.getSource());
        assertEquals(OnOffType.class, itemCommandEvent.getItemCommand().getClass());
        assertEquals(ITEM_COMMAND, itemCommandEvent.getItemCommand());
    }

    @Test
    public void testCreateCommandEventOnOffType() throws Exception {
        ItemCommandEvent event = ItemEventFactory.createCommandEvent(ITEM_NAME, ITEM_COMMAND, SOURCE);

        assertEquals(ITEM_COMMAND_EVENT_TYPE, event.getType());
        assertEquals(ITEM_COMMAND_EVENT_TOPIC, event.getTopic());
        assertEquals(ITEM_COMMAND_EVENT_PAYLOAD, event.getPayload());
        assertEquals(ITEM_NAME, event.getItemName());
        assertEquals(SOURCE, event.getSource());
        assertEquals(OnOffType.class, event.getItemCommand().getClass());
        assertEquals(ITEM_COMMAND, event.getItemCommand());
    }

    @Test
    public void testCreateEventItemCommandEventRefreshType() throws Exception {
        Event event = factory.createEvent(ITEM_COMMAND_EVENT_TYPE, ITEM_COMMAND_EVENT_TOPIC,
                ITEM_REFRESH_COMMAND_EVENT_PAYLOAD, SOURCE);

        assertEquals(ItemCommandEvent.class, event.getClass());
        ItemCommandEvent itemCommandEvent = (ItemCommandEvent) event;
        assertEquals(ITEM_COMMAND_EVENT_TYPE, itemCommandEvent.getType());
        assertEquals(ITEM_COMMAND_EVENT_TOPIC, itemCommandEvent.getTopic());
        assertEquals(ITEM_REFRESH_COMMAND_EVENT_PAYLOAD, itemCommandEvent.getPayload());
        assertEquals(ITEM_NAME, itemCommandEvent.getItemName());
        assertEquals(SOURCE, itemCommandEvent.getSource());
        assertEquals(RefreshType.REFRESH, itemCommandEvent.getItemCommand());
    }

    @Test
    public void testCreateEventItemStateEventUnDefType() throws Exception {
        Event event = factory.createEvent(ITEM_STATE_EVENT_TYPE, ITEM_STATE_EVENT_TOPIC, ITEM_UNDEF_STATE_EVENT_PAYLOAD,
                SOURCE);

        assertEquals(ItemStateEvent.class, event.getClass());
        ItemStateEvent itemStateEvent = (ItemStateEvent) event;

        assertEquals(ITEM_STATE_EVENT_TYPE, itemStateEvent.getType());
        assertEquals(ITEM_STATE_EVENT_TOPIC, itemStateEvent.getTopic());
        assertEquals(ITEM_UNDEF_STATE_EVENT_PAYLOAD, itemStateEvent.getPayload());
        assertEquals(ITEM_NAME, itemStateEvent.getItemName());
        assertEquals(SOURCE, itemStateEvent.getSource());
        assertEquals(UnDefType.UNDEF, itemStateEvent.getItemState());
    }

    @Test
    public void testCreateEventGroupItemStateChangedEvent() throws Exception {
        Event event = factory.createEvent(GROUPITEM_CHANGED_EVENT_TYPE, GROUPITEM_STATE_CHANGED_EVENT_TOPIC,
                ITEM_STATE_CHANGED_EVENT_PAYLOAD, SOURCE);

        assertEquals(GroupItemStateChangedEvent.class, event.getClass());
        GroupItemStateChangedEvent groupItemStateChangedEvent = (GroupItemStateChangedEvent) event;

        assertEquals(GROUPITEM_CHANGED_EVENT_TYPE, groupItemStateChangedEvent.getType());
        assertEquals(GROUPITEM_STATE_CHANGED_EVENT_TOPIC, groupItemStateChangedEvent.getTopic());
        assertEquals(ITEM_STATE_CHANGED_EVENT_PAYLOAD, groupItemStateChangedEvent.getPayload());
        assertEquals(GROUP_NAME, groupItemStateChangedEvent.getItemName());
        assertEquals(ITEM_NAME, groupItemStateChangedEvent.getMemberName());
        assertNull(groupItemStateChangedEvent.getSource());
        assertEquals(NEW_ITEM_STATE, groupItemStateChangedEvent.getItemState());
        assertEquals(ITEM_STATE, groupItemStateChangedEvent.getOldItemState());
    }

    @Test
    public void testCreateEventItemStateEventOnOffType() throws Exception {
        Event event = factory.createEvent(ITEM_STATE_EVENT_TYPE, ITEM_STATE_EVENT_TOPIC, ITEM_STATE_EVENT_PAYLOAD,
                SOURCE);

        assertEquals(ItemStateEvent.class, event.getClass());
        ItemStateEvent itemStateEvent = (ItemStateEvent) event;
        assertEquals(ITEM_STATE_EVENT_TYPE, itemStateEvent.getType());
        assertEquals(ITEM_STATE_EVENT_TOPIC, itemStateEvent.getTopic());
        assertEquals(ITEM_STATE_EVENT_PAYLOAD, itemStateEvent.getPayload());
        assertEquals(ITEM_NAME, itemStateEvent.getItemName());
        assertEquals(SOURCE, itemStateEvent.getSource());
        assertEquals(OnOffType.class, itemStateEvent.getItemState().getClass());
        assertEquals(ITEM_STATE, itemStateEvent.getItemState());
    }

    @Test
    public void testCreateEventItemStatePredictedEventOnOffType() throws Exception {
        Event event = factory.createEvent(ITEM_STATE_PREDICTED_EVENT_TYPE, ITEM_STATE_PREDICTED_EVENT_TOPIC,
                ITEM_STATE_PREDICTED_EVENT_PAYLOAD, SOURCE);

        assertEquals(ItemStatePredictedEvent.class, event.getClass());
        ItemStatePredictedEvent itemStatePredictedEvent = (ItemStatePredictedEvent) event;
        assertEquals(ITEM_STATE_PREDICTED_EVENT_TYPE, itemStatePredictedEvent.getType());
        assertEquals(ITEM_STATE_PREDICTED_EVENT_TOPIC, itemStatePredictedEvent.getTopic());
        assertEquals(ITEM_STATE_PREDICTED_EVENT_PAYLOAD, itemStatePredictedEvent.getPayload());
        assertEquals(ITEM_NAME, itemStatePredictedEvent.getItemName());
        assertEquals(OnOffType.class, itemStatePredictedEvent.getPredictedState().getClass());
        assertEquals(ITEM_STATE, itemStatePredictedEvent.getPredictedState());
    }

    @Test
    public void testCreateStateEventOnOffType() {
        ItemStateEvent event = ItemEventFactory.createStateEvent(ITEM_NAME, ITEM_STATE, SOURCE);

        assertThat(event.getType(), is(ITEM_STATE_EVENT_TYPE));
        assertThat(event.getTopic(), is(ITEM_STATE_EVENT_TOPIC));
        assertThat(event.getPayload(), is(ITEM_STATE_EVENT_PAYLOAD));
        assertThat(event.getItemName(), is(ITEM_NAME));
        assertThat(event.getSource(), is(SOURCE));
        assertEquals(OnOffType.class, event.getItemState().getClass());
        assertThat(event.getItemState(), is(ITEM_STATE));
    }

    @Test
    public void testCreateEventItemAddedEvent() throws Exception {
        Event event = factory.createEvent(ITEM_ADDED_EVENT_TYPE, ITEM_ADDED_EVENT_TOPIC, ITEM_ADDED_EVENT_PAYLOAD,
                null);

        assertEquals(ItemAddedEvent.class, event.getClass());
        ItemAddedEvent itemAddedEvent = (ItemAddedEvent) event;
        assertEquals(ITEM_ADDED_EVENT_TYPE, itemAddedEvent.getType());
        assertEquals(ITEM_ADDED_EVENT_TOPIC, itemAddedEvent.getTopic());
        assertEquals(ITEM_ADDED_EVENT_PAYLOAD, itemAddedEvent.getPayload());
        assertNotNull(itemAddedEvent.getItem());
        assertEquals(ITEM_NAME, itemAddedEvent.getItem().name);
        assertEquals(CoreItemFactory.SWITCH, itemAddedEvent.getItem().type);
    }

    @Test
    public void testCreateAddedEvent() {
        ItemAddedEvent event = ItemEventFactory.createAddedEvent(ITEM);

        assertEquals(ItemAddedEvent.TYPE, event.getType());
        assertEquals(ITEM_ADDED_EVENT_TOPIC, event.getTopic());
        assertNotNull(event.getItem());
        assertEquals(ITEM_NAME, event.getItem().name);
        assertEquals(CoreItemFactory.SWITCH, event.getItem().type);
    }

    @Test
    public void testCreateGroupStateChangedEventRawType() throws Exception {
        GroupItemStateChangedEvent giEventSource = ItemEventFactory.createGroupStateChangedEvent(GROUP_NAME, ITEM_NAME,
                NEW_RAW_ITEM_STATE, RAW_ITEM_STATE);

        Event giEventParsed = factory.createEvent(giEventSource.getType(), giEventSource.getTopic(),
                giEventSource.getPayload(), giEventSource.getSource());

        assertEquals(GroupItemStateChangedEvent.class, giEventParsed.getClass());
        GroupItemStateChangedEvent groupItemStateChangedEvent = (GroupItemStateChangedEvent) giEventParsed;

        assertEquals(GROUPITEM_CHANGED_EVENT_TYPE, groupItemStateChangedEvent.getType());
        assertEquals(GROUPITEM_STATE_CHANGED_EVENT_TOPIC, groupItemStateChangedEvent.getTopic());
        assertEquals(giEventSource.getPayload(), groupItemStateChangedEvent.getPayload());
        assertEquals(GROUP_NAME, groupItemStateChangedEvent.getItemName());
        assertEquals(ITEM_NAME, groupItemStateChangedEvent.getMemberName());
        assertNull(groupItemStateChangedEvent.getSource());
        assertEquals(NEW_RAW_ITEM_STATE, groupItemStateChangedEvent.getItemState());
        assertEquals(RAW_ITEM_STATE, groupItemStateChangedEvent.getOldItemState());
    }
}
