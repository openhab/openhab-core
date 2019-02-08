/**
 * Copyright (c) 2014,2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.core.items;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.smarthome.core.events.EventPublisher;
import org.eclipse.smarthome.core.i18n.UnitProvider;
import org.eclipse.smarthome.core.items.events.ItemStateChangedEvent;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.eclipse.smarthome.core.library.types.RawType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.service.CommandDescriptionService;
import org.eclipse.smarthome.core.service.StateDescriptionService;
import org.eclipse.smarthome.core.types.CommandDescription;
import org.eclipse.smarthome.core.types.CommandOption;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.core.types.StateDescriptionFragmentBuilder;
import org.eclipse.smarthome.core.types.StateOption;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

/**
 * The GenericItemTest tests functionality of the GenericItem.
 *
 * @author Christoph Knauf - Initial contribution, event tests
 * @author Simon Kaufmann - migrated from Groovy to Java
 */
@SuppressWarnings("null")
public class GenericItemTest {

    @Test
    public void testItemPostsEventsCorrectly() {
        EventPublisher publisher = mock(EventPublisher.class);

        TestItem item = new TestItem("member1");
        item.setEventPublisher(publisher);
        State oldState = item.getState();

        // State changes -> one change event is fired
        item.setState(new RawType(new byte[0], RawType.DEFAULT_MIME_TYPE));

        ArgumentCaptor<ItemStateChangedEvent> captor = ArgumentCaptor.forClass(ItemStateChangedEvent.class);

        verify(publisher, times(1)).post(captor.capture());

        ItemStateChangedEvent change = captor.getValue();

        assertEquals(item.getName(), change.getItemName());
        assertEquals("smarthome/items/member1/statechanged", change.getTopic());
        assertEquals(oldState, change.getOldItemState());
        assertEquals(item.getState(), change.getItemState());
        assertEquals(ItemStateChangedEvent.TYPE, change.getType());

        // State doesn't change -> no event is fired
        item.setState(item.getState());
        verifyNoMoreInteractions(publisher);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddGroupNameWithNull() {
        TestItem item = new TestItem("member1");
        item.addGroupName(NULL());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAddGroupNamesWithNull() {
        TestItem item = new TestItem("member1");
        item.addGroupNames(Arrays.asList("group-a", NULL(), "group-b"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRemoveGroupNameWithNull() {
        TestItem item = new TestItem("member1");
        item.removeGroupName(NULL());
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
        assertNull(item.getStateAs(NULL()));
    }

    @Test
    public void testDispose() {
        TestItem item = new TestItem("test");
        item.setEventPublisher(mock(EventPublisher.class));
        item.setItemStateConverter(mock(ItemStateConverter.class));
        item.setStateDescriptionService(null);
        item.setUnitProvider(mock(UnitProvider.class));

        item.addStateChangeListener(mock(StateChangeListener.class));

        item.dispose();

        assertNull(item.eventPublisher);
        assertNull(item.itemStateConverter);
        // can not be tested as stateDescriptionProviders is private in GenericItem
        // assertThat(item.stateDescriptionProviders, is(nullValue()));
        assertNull(item.unitProvider);
        assertEquals(0, item.listeners.size());
    }

    @Test
    public void testCommandDescription() {
        TestItem item = new TestItem("test");

        CommandDescriptionService commandDescriptionService = mock(CommandDescriptionService.class);
        when(commandDescriptionService.getCommandDescription("test", null)).thenReturn(new CommandDescription() {

            @Override
            public @NonNull List<@NonNull CommandOption> getCommandOptions() {
                return Arrays.asList(new CommandOption("ALERT", "Alert"), new CommandOption("REBOOT", "Reboot"));
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
                    public @NonNull List<@NonNull CommandOption> getCommandOptions() {
                        return Arrays.asList(new CommandOption("C1", "Command 1"), new CommandOption("C2", "Command 2"),
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
        List<@NonNull StateOption> stateOptions = Arrays.asList(new StateOption("STATE1", "State 1"),
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
    private <T> T NULL() {
        return null; // :-P
    }

}
