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

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;

import org.eclipse.smarthome.core.events.EventPublisher;
import org.eclipse.smarthome.core.i18n.UnitProvider;
import org.eclipse.smarthome.core.items.events.ItemStateChangedEvent;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.eclipse.smarthome.core.library.types.RawType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.types.State;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

/**
 * The GenericItemTest tests functionality of the GenericItem.
 *
 * @author Christoph Knauf - Initial contribution, event tests
 * @author Simon Kaufmann - migrated from Groovy to Java
 */
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

    /**
     * Fooling the null-analysis tooling
     *
     * @return always {@code null}
     */
    private <T> T NULL() {
        return null; // :-P
    }

}
