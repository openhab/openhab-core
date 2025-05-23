/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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
package org.openhab.core.items;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Locale;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.items.events.ItemEvent;
import org.openhab.core.items.events.ItemStateChangedEvent;
import org.openhab.core.items.events.ItemStateUpdatedEvent;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.library.types.RawType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.service.CommandDescriptionService;
import org.openhab.core.service.StateDescriptionService;
import org.openhab.core.types.CommandDescription;
import org.openhab.core.types.CommandOption;
import org.openhab.core.types.State;
import org.openhab.core.types.StateDescriptionFragmentBuilder;
import org.openhab.core.types.StateOption;
import org.openhab.core.types.UnDefType;

/**
 * The GenericItemTest tests functionality of the GenericItem.
 *
 * @author Christoph Knauf - Initial contribution, event tests
 * @author Simon Kaufmann - migrated from Groovy to Java
 */
@NonNullByDefault
@SuppressWarnings("null")
public class GenericItemTest {

    @Test
    public void testItemPostsEventsCorrectly() {
        ZonedDateTime lastStateUpdate;
        ZonedDateTime lastStateChange;
        EventPublisher publisher = mock(EventPublisher.class);

        TestItem item = new TestItem("member1");
        item.setEventPublisher(publisher);
        State oldState = item.getState();

        // State changes -> one update and one change event is fired
        item.setState(new RawType(new byte[0], RawType.DEFAULT_MIME_TYPE));

        ArgumentCaptor<ItemEvent> captor = ArgumentCaptor.forClass(ItemEvent.class);

        verify(publisher, times(2)).post(captor.capture());

        List<ItemEvent> events = captor.getAllValues();
        assertEquals(2, events.size());

        // first event should be updated event
        assertInstanceOf(ItemStateUpdatedEvent.class, events.getFirst());
        ItemStateUpdatedEvent updated = (ItemStateUpdatedEvent) events.getFirst();
        assertEquals(item.getName(), updated.getItemName());
        assertEquals("openhab/items/member1/stateupdated", updated.getTopic());
        assertEquals(item.getState(), updated.getItemState());
        assertEquals(null, updated.getLastStateUpdate()); // this is the first update, so there is no previous update
        assertEquals(ItemStateUpdatedEvent.TYPE, updated.getType());

        // second event should be changed event
        assertInstanceOf(ItemStateChangedEvent.class, events.get(1));
        ItemStateChangedEvent change = (ItemStateChangedEvent) events.get(1);
        assertEquals(item.getName(), change.getItemName());
        assertEquals("openhab/items/member1/statechanged", change.getTopic());
        assertEquals(oldState, change.getOldItemState());
        assertEquals(item.getState(), change.getItemState());
        assertEquals(null, change.getLastStateChange()); // this is the first change, so there is no previous change
        assertEquals(ItemStateChangedEvent.TYPE, change.getType());

        // reset invocations and captor
        clearInvocations(publisher);
        captor = ArgumentCaptor.forClass(ItemEvent.class);

        lastStateChange = item.getLastStateChange();
        lastStateUpdate = item.getLastStateUpdate();

        // State doesn't change -> only update event is fired
        item.setState(item.getState());
        verify(publisher).post(captor.capture());

        events = captor.getAllValues();
        assertEquals(1, events.size()); // two before and one additional

        // event should be updated event
        assertInstanceOf(ItemStateUpdatedEvent.class, events.getFirst());
        updated = (ItemStateUpdatedEvent) events.getFirst();
        assertEquals(item.getName(), updated.getItemName());
        assertEquals("openhab/items/member1/stateupdated", updated.getTopic());
        assertEquals(item.getState(), updated.getItemState());
        assertEquals(lastStateUpdate, updated.getLastStateUpdate());
        assertEquals(ItemStateUpdatedEvent.TYPE, updated.getType());

        // State changes -> the ItemStateChangedEvent should include the lastStateChange
        clearInvocations(publisher);
        captor = ArgumentCaptor.forClass(ItemEvent.class);

        lastStateUpdate = item.getLastStateUpdate();

        // New State
        item.setState(new RawType(new byte[1], RawType.DEFAULT_MIME_TYPE));
        verify(publisher, times(2)).post(captor.capture());

        events = captor.getAllValues();
        assertEquals(2, events.size());
        assertInstanceOf(ItemStateChangedEvent.class, events.get(1));
        change = (ItemStateChangedEvent) events.get(1);
        assertEquals(lastStateUpdate, change.getLastStateUpdate());
        assertEquals(lastStateChange, change.getLastStateChange());
    }

    @Test
    public void testGetStateAsWithSameType() {
        TestItem item = new TestItem("member1");
        item.setState(PercentType.HUNDRED);
        assertEquals(PercentType.class, item.getStateAs(PercentType.class).getClass());
    }

    @Test
    public void testGetStateAsWithDifferentType() {
        TestItem item = new TestItem("member1");
        item.setState(PercentType.HUNDRED);
        assertEquals(OnOffType.class, item.getStateAs(OnOffType.class).getClass());
    }

    @Test
    public void testGetStateAsWithNonConvertible() {
        TestItem item = new TestItem("member1");
        item.setState(StringType.valueOf("Hello World"));
        assertEquals(StringType.class, item.getStateAs(StringType.class).getClass());
    }

    @Test
    public void testGetStateAsWithNull() {
        TestItem item = new TestItem("member1");
        item.setState(StringType.valueOf("Hello World"));
        assertNull(item.getStateAs(toNull()));
    }

    @Test
    public void testGetLastStateUpdate() {
        TestItem item = new TestItem("member1");
        assertNull(item.getLastStateUpdate());
        item.setState(PercentType.HUNDRED);
        assertThat(item.getLastStateUpdate().toInstant().toEpochMilli() * 1.0,
                is(closeTo(ZonedDateTime.now().toInstant().toEpochMilli(), 5)));
    }

    @Test
    public void testGetLastStateChange() throws InterruptedException {
        TestItem item = new TestItem("member1");
        assertNull(item.getLastStateChange());
        item.setState(PercentType.HUNDRED);
        ZonedDateTime initialChangeTime = ZonedDateTime.now();
        assertThat(item.getLastStateChange().toInstant().toEpochMilli() * 1.0,
                is(closeTo(initialChangeTime.toInstant().toEpochMilli(), 5)));

        Thread.sleep(50);
        item.setState(PercentType.HUNDRED);
        assertThat(item.getLastStateChange().toInstant().toEpochMilli() * 1.0,
                is(closeTo(initialChangeTime.toInstant().toEpochMilli(), 5)));

        Thread.sleep(50);
        ZonedDateTime secondChangeTime = ZonedDateTime.now();
        item.setState(PercentType.ZERO);
        assertThat(item.getLastStateChange().toInstant().toEpochMilli() * 1.0,
                is(closeTo(secondChangeTime.toInstant().toEpochMilli(), 5)));
    }

    @Test
    public void testGetLastState() {
        TestItem item = new TestItem("member1");
        assertEquals(UnDefType.NULL, item.getState());
        assertNull(item.getLastState());
        item.setState(PercentType.HUNDRED);
        assertEquals(UnDefType.NULL, item.getLastState());
        item.setState(PercentType.ZERO);
        assertEquals(PercentType.HUNDRED, item.getLastState());
    }

    @Test
    public void testDispose() {
        TestItem item = new TestItem("test");
        item.setEventPublisher(mock(EventPublisher.class));
        item.setItemStateConverter(mock(ItemStateConverter.class));
        item.setStateDescriptionService(null);

        item.addStateChangeListener(mock(StateChangeListener.class));

        item.dispose();

        assertNull(item.eventPublisher);
        assertNull(item.itemStateConverter);
        // can not be tested as stateDescriptionProviders is private in GenericItem
        // assertThat(item.stateDescriptionProviders, is(nullValue()));
        assertEquals(0, item.listeners.size());
    }

    @Test
    public void testCommandDescription() {
        TestItem item = new TestItem("test");

        CommandDescriptionService commandDescriptionService = mock(CommandDescriptionService.class);
        when(commandDescriptionService.getCommandDescription("test", null)).thenReturn(new CommandDescription() {

            @Override
            public List<CommandOption> getCommandOptions() {
                return List.of(new CommandOption("ALERT", "Alert"), new CommandOption("REBOOT", "Reboot"));
            }
        });
        item.setCommandDescriptionService(commandDescriptionService);

        assertThat(item.getCommandDescription().getCommandOptions(), hasSize(2));
    }

    @Test
    public void testCommandDescriptionWithLocale() {
        TestItem item = new TestItem("test");

        CommandDescriptionService commandDescriptionService = mock(CommandDescriptionService.class);
        when(commandDescriptionService.getCommandDescription(eq("test"), any(Locale.class)))
                .thenReturn(new CommandDescription() {

                    @Override
                    public List<CommandOption> getCommandOptions() {
                        return List.of(new CommandOption("C1", "Command 1"), new CommandOption("C2", "Command 2"),
                                new CommandOption("C3", "Command 3"));
                    }
                });
        item.setCommandDescriptionService(commandDescriptionService);

        assertThat(item.getCommandDescription(Locale.getDefault()).getCommandOptions(), hasSize(3));
    }

    @Test
    public void commandDescriptionShouldHaveStateOptionsAsCommands() {
        TestItem item = new TestItem("test");

        StateDescriptionService stateDescriptionService = mock(StateDescriptionService.class);
        List<StateOption> stateOptions = List.of(new StateOption("STATE1", "State 1"),
                new StateOption("STATE2", "State 2"));
        when(stateDescriptionService.getStateDescription("test", null)).thenReturn(
                StateDescriptionFragmentBuilder.create().withOptions(stateOptions).build().toStateDescription());
        item.setStateDescriptionService(stateDescriptionService);

        assertThat(item.getCommandDescription().getCommandOptions(), hasSize(2));
    }

    /**
     * Fooling the null-analysis tooling
     *
     * @return always {@code null}
     */
    private <T> T toNull() {
        return null; // :-P
    }
}
