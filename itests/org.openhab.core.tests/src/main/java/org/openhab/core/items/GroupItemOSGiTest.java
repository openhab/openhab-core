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
package org.openhab.core.items;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.measure.Quantity;
import javax.measure.quantity.Dimensionless;
import javax.measure.quantity.Pressure;
import javax.measure.quantity.Temperature;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import org.openhab.core.events.Event;
import org.openhab.core.events.EventFilter;
import org.openhab.core.events.EventPublisher;
import org.openhab.core.events.EventSubscriber;
import org.openhab.core.i18n.UnitProvider;
import org.openhab.core.internal.items.GroupFunctionHelper;
import org.openhab.core.internal.items.ItemStateConverterImpl;
import org.openhab.core.items.dto.GroupFunctionDTO;
import org.openhab.core.items.events.GroupItemStateChangedEvent;
import org.openhab.core.items.events.ItemCommandEvent;
import org.openhab.core.items.events.ItemUpdatedEvent;
import org.openhab.core.library.CoreItemFactory;
import org.openhab.core.library.items.ColorItem;
import org.openhab.core.library.items.DimmerItem;
import org.openhab.core.library.items.NumberItem;
import org.openhab.core.library.items.RollershutterItem;
import org.openhab.core.library.items.SwitchItem;
import org.openhab.core.library.types.ArithmeticGroupFunction;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.HSBType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.types.RawType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.test.java.JavaOSGiTest;
import org.openhab.core.types.RefreshType;
import org.openhab.core.types.State;
import org.openhab.core.types.UnDefType;

import tec.uom.se.unit.Units;

/**
 * @author Stefan Triller - Initial contribution
 */
public class GroupItemOSGiTest extends JavaOSGiTest {

    /** Time to sleep when a file is created/modified/deleted, so the event can be handled */
    private static final int WAIT_EVENT_TO_BE_HANDLED = 1000;

    private List<Event> events = new LinkedList<>();
    private EventPublisher publisher;

    private ItemRegistry itemRegistry;

    @Mock
    private UnitProvider unitProvider;

    private GroupFunctionHelper groupFunctionHelper;
    private ItemStateConverter itemStateConverter;

    @Before
    public void setUp() {
        initMocks(this);
        registerVolatileStorageService();
        publisher = event -> events.add(event);

        itemRegistry = getService(ItemRegistry.class);
        assertNotNull(itemRegistry);

        registerService(new EventSubscriber() {

            @Override
            public void receive(Event event) {
                events.add(event);
            }

            @Override
            public Set<String> getSubscribedEventTypes() {
                Set<String> hs = new HashSet<>();
                hs.add(ItemUpdatedEvent.TYPE);
                return hs;
            }

            @Override
            public EventFilter getEventFilter() {
                return null;
            }
        });

        when(unitProvider.getUnit(Temperature.class)).thenReturn(Units.CELSIUS);

        groupFunctionHelper = new GroupFunctionHelper();
        itemStateConverter = new ItemStateConverterImpl(unitProvider);
    }

    @Ignore
    @Test
    public void testItemUpdateWithItemRegistry() {
        GroupItem item = new GroupItem("mySimpleGroupItem");
        item.setLabel("firstLabel");

        itemRegistry.add(item);

        GroupItem updatedItem = (GroupItem) itemRegistry.get("mySimpleGroupItem");
        assertNotNull(updatedItem);

        events.clear();

        updatedItem.setLabel("secondLabel");
        itemRegistry.update(updatedItem);
        waitForAssert(() -> assertThat(events.size(), is(1)));

        List<Event> stateChanges = events.stream().filter(it -> it instanceof ItemUpdatedEvent)
                .collect(Collectors.toList());
        assertThat(stateChanges.size(), is(1));

        ItemUpdatedEvent change = (ItemUpdatedEvent) stateChanges.get(0);

        assertThat(change.getItem().label, is("secondLabel"));
    }

    @SuppressWarnings("unchecked")
    @Test()
    public void assertAcceptedCommandTypesOnGroupItemsReturnsSubsetOfCommandTypesSupportedByAllMembers() {
        SwitchItem switchItem = new SwitchItem("switch");
        NumberItem numberItem = new NumberItem("number");

        GroupItem groupItem = new GroupItem("group");
        groupItem.addMember(switchItem);
        groupItem.addMember(numberItem);

        assertThat(groupItem.getAcceptedCommandTypes(), hasItems(RefreshType.class));
    }

    @Test
    public void testGetAllMembers() {
        GroupItem rootGroupItem = new GroupItem("root");
        rootGroupItem.addMember(new TestItem("member1"));
        rootGroupItem.addMember(new TestItem("member2"));
        rootGroupItem.addMember(new TestItem("member2"));
        GroupItem subGroup = new GroupItem("subGroup1");
        subGroup.addMember(new TestItem("subGroup member 1"));
        subGroup.addMember(new TestItem("subGroup member 2"));
        subGroup.addMember(new TestItem("subGroup member 3"));
        subGroup.addMember(new TestItem("member1"));
        rootGroupItem.addMember(subGroup);
        assertThat(rootGroupItem.getAllMembers().size(), is(5));
        for (Item member : rootGroupItem.getAllMembers()) {
            if (member instanceof GroupItem) {
                fail("There are no GroupItems allowed in this Collection");
            }
        }
    }

    @Test
    public void testRemoveMemberUnregistersListenerFromMember() {
        GroupItem group = new GroupItem("group");
        TestItem member1 = new TestItem("member1");
        TestItem member2 = new TestItem("member2");

        group.addMember(member1);
        group.addMember(member2);

        assertThat(member1.getListeners(), hasSize(1));
        assertThat(member2.getListeners(), hasSize(1));
        assertThat(group.getMembers(), hasSize(2));

        group.removeMember(member1);
        assertThat(member1.getListeners(), hasSize(0));
        assertThat(member2.getListeners(), hasSize(1));
        assertThat(group.getMembers(), hasSize(1));
    }

    @Test
    public void testRemoveAllMembersUnregistersListenerFromAllMembers() {
        GroupItem group = new GroupItem("group");
        TestItem member1 = new TestItem("member1");
        TestItem member2 = new TestItem("member2");

        group.addMember(member1);
        group.addMember(member2);

        assertThat(member1.getListeners(), hasSize(1));
        assertThat(member2.getListeners(), hasSize(1));
        assertThat(group.getMembers(), hasSize(2));

        group.removeAllMembers();
        assertThat(member1.getListeners(), hasSize(0));
        assertThat(member2.getListeners(), hasSize(0));
        assertThat(group.getMembers(), hasSize(0));
    }

    @Test
    public void testDisposeUnregistersListenerFromAllMembers() {
        GroupItem group = new GroupItem("group");
        TestItem member1 = new TestItem("member1");
        TestItem member2 = new TestItem("member2");

        group.addMember(member1);
        group.addMember(member2);

        assertThat(member1.getListeners(), hasSize(1));
        assertThat(member2.getListeners(), hasSize(1));
        assertThat(group.getMembers(), hasSize(2));

        group.dispose();
        assertThat(member1.getListeners(), hasSize(0));
        assertThat(member2.getListeners(), hasSize(0));
        assertThat(group.getMembers(), hasSize(0));
    }

    @Test
    public void testGetAllMembersWithCircleDependency() {
        GroupItem rootGroupItem = new GroupItem("root");
        rootGroupItem.addMember(new TestItem("member1"));
        rootGroupItem.addMember(new TestItem("member2"));
        GroupItem subGroup = new GroupItem("subGroup1");
        subGroup.addMember(new TestItem("subGroup member 1"));
        subGroup.addMember(rootGroupItem);
        rootGroupItem.addMember(subGroup);
        assertThat(rootGroupItem.getAllMembers().size(), is(3));
        for (Item member : rootGroupItem.getAllMembers()) {
            if (member instanceof GroupItem) {
                fail("There are no GroupItems allowed in this Collection");
            }
        }
    }

    @SuppressWarnings("null")
    @Test
    public void testGetAllMembersWithFilter() {
        GroupItem rootGroupItem = new GroupItem("root");

        TestItem member1 = new TestItem("member1");
        member1.setLabel("mem1");
        rootGroupItem.addMember(member1);

        TestItem member2 = new TestItem("member2");
        member2.setLabel("mem1");
        rootGroupItem.addMember(member2);

        TestItem member3 = new TestItem("member3");
        member3.setLabel("mem3");
        rootGroupItem.addMember(member3);

        GroupItem subGroup = new GroupItem("subGroup1");
        subGroup.setLabel("subGrp1");
        TestItem subMember1 = new TestItem("subGroup member 1");
        subMember1.setLabel("subMem1");
        subGroup.addMember(subMember1);
        TestItem subMember2 = new TestItem("subGroup member 2");
        subMember2.setLabel("subMem2");
        subGroup.addMember(subMember2);
        TestItem subMember3 = new TestItem("subGroup member 3");
        subMember3.setLabel("subMem3");
        subGroup.addMember(subMember3);
        subGroup.addMember(member1);
        rootGroupItem.addMember(subGroup);

        Set<Item> members = rootGroupItem.getMembers(i -> i instanceof GroupItem);
        assertThat(members.size(), is(1));

        members = rootGroupItem.getMembers(i -> i.getLabel().equals("mem1"));
        assertThat(members.size(), is(2));
    }

    @Test
    public void testGetMembersFilteringGroups() {
        GroupItem rootGroupItem = new GroupItem("root");

        TestItem member1 = new TestItem("member1");
        member1.setLabel("mem1");
        rootGroupItem.addMember(member1);

        GroupItem subGroup = new GroupItem("subGroup1");
        subGroup.setLabel("subGrp1");
        TestItem subMember1 = new TestItem("subGroup member 1");
        subMember1.setLabel("subMem1");
        subGroup.addMember(subMember1);

        rootGroupItem.addMember(subGroup);

        Set<Item> members = rootGroupItem
                .getMembers(i -> !(i instanceof GroupItem) && i.getGroupNames().contains(rootGroupItem.getName()));
        assertThat(members.size(), is(1));
    }

    @Test
    public void testGetMembersFilteringGroupsTransient() {
        GroupItem rootGroupItem = new GroupItem("root");

        TestItem member1 = new TestItem("member1");
        member1.setLabel("mem1");
        rootGroupItem.addMember(member1);

        GroupItem subGroup = new GroupItem("subGroup1");
        subGroup.setLabel("subGrp1");
        TestItem subMember1 = new TestItem("subGroup member 1");
        subMember1.setLabel("subMem1");
        subGroup.addMember(subMember1);

        rootGroupItem.addMember(subGroup);

        Set<Item> members = rootGroupItem.getMembers(i -> !(i instanceof GroupItem));
        assertThat(members.size(), is(2));
    }

    @Test
    public void testGetStateAsShouldEqualStateUpdate() {
        // Main group uses AND function
        GroupItem rootGroupItem = new GroupItem("root", new SwitchItem("baseItem"),
                new ArithmeticGroupFunction.And(OnOffType.ON, OnOffType.OFF));
        rootGroupItem.setItemStateConverter(itemStateConverter);

        TestItem member1 = new TestItem("member1");
        rootGroupItem.addMember(member1);
        TestItem member2 = new TestItem("member2");
        rootGroupItem.addMember(member2);

        // Sub-group uses NAND function
        GroupItem subGroup = new GroupItem("subGroup1", new SwitchItem("baseItem"),
                new ArithmeticGroupFunction.NAnd(OnOffType.ON, OnOffType.OFF));
        TestItem subMember = new TestItem("subGroup member 1");
        subGroup.addMember(subMember);
        rootGroupItem.addMember(subGroup);

        member1.setState(OnOffType.ON);
        member2.setState(OnOffType.ON);
        subMember.setState(OnOffType.OFF);

        // subGroup and subMember state differ
        assertThat(subGroup.getStateAs(OnOffType.class), is(OnOffType.ON));
        assertThat(subMember.getStateAs(OnOffType.class), is(OnOffType.OFF));

        // We expect ON here
        State getStateAsState = rootGroupItem.getStateAs(OnOffType.class);

        rootGroupItem.stateUpdated(member1, UnDefType.NULL); // recalculate the state
        State stateUpdatedState = rootGroupItem.getState();

        assertThat(getStateAsState, is(OnOffType.ON));
        assertThat(stateUpdatedState, is(OnOffType.ON));
    }

    @Test
    public void assertCyclicGroupItemsCalculateState() {
        GroupFunction countFn = new ArithmeticGroupFunction.Count(new StringType(".*"));
        GroupItem rootGroup = new GroupItem("rootGroup", new SwitchItem("baseItem"), countFn);
        TestItem rootMember = new TestItem("rootMember");
        rootGroup.addMember(rootMember);

        GroupItem group1 = new GroupItem("group1");
        GroupItem group2 = new GroupItem("group2");

        rootGroup.addMember(group1);
        group1.addMember(group2);
        group2.addMember(group1);

        group1.addMember(new TestItem("sub1"));
        group2.addMember(new TestItem("sub2-1"));
        group2.addMember(new TestItem("sub2-2"));
        group2.addMember(new TestItem("sub2-3"));

        // count: rootMember, sub1, sub2-1, sub2-2, sub2-3
        DecimalType state = rootGroup.getStateAs(DecimalType.class);
        assertThat(state, is(notNullValue()));
        if (state != null) {
            assertThat(state, is(new DecimalType(5)));
        }
    }

    @Test
    public void assertCyclicGroupItemsCalculateStateWithSubGroupFunction() {
        GroupFunction countFn = new ArithmeticGroupFunction.Count(new StringType(".*"));
        GroupItem rootGroup = new GroupItem("rootGroup", new SwitchItem("baseItem"), countFn);
        TestItem rootMember = new TestItem("rootMember");
        rootGroup.addMember(rootMember);

        GroupItem group1 = new GroupItem("group1");
        GroupItem group2 = new GroupItem("group2", new SwitchItem("baseItem"), new ArithmeticGroupFunction.Sum());

        rootGroup.addMember(group1);
        group1.addMember(group2);
        group2.addMember(group1);

        group1.addMember(new TestItem("sub1"));
        group2.addMember(new TestItem("sub2-1"));
        group2.addMember(new TestItem("sub2-2"));
        group2.addMember(new TestItem("sub2-3"));

        // count: rootMember, sub1, group2
        DecimalType state = rootGroup.getStateAs(DecimalType.class);
        assertThat(state, is(notNullValue()));
        if (state != null) {
            assertThat(state, is(new DecimalType(3)));
        }
    }

    @Test
    public void assertThatGroupItemPostsEventsForChangesCorrectly() {
        // from ItemEventFactory.GROUPITEM_STATE_CHANGED_EVENT_TOPIC
        String groupitemStateChangedEventTopic = "smarthome/items/{itemName}/{memberName}/statechanged";

        events.clear();
        GroupItem groupItem = new GroupItem("root", new SwitchItem("mySwitch"), new GroupFunction.Equality());
        groupItem.setItemStateConverter(itemStateConverter);

        SwitchItem member = new SwitchItem("member1");
        groupItem.addMember(member);
        groupItem.setEventPublisher(publisher);
        State oldGroupState = groupItem.getState();

        // State changes -> one change event is fired
        member.setState(OnOffType.ON);

        waitForAssert(() -> assertThat(events.size(), is(1)));

        List<Event> changes = events.stream().filter(it -> it instanceof GroupItemStateChangedEvent)
                .collect(Collectors.toList());
        assertThat(changes.size(), is(1));

        GroupItemStateChangedEvent change = (GroupItemStateChangedEvent) changes.get(0);
        assertTrue(change.getItemName().equals(groupItem.getName()));
        assertTrue(change.getMemberName().equals(member.getName()));
        assertTrue(change.getTopic().equals(groupitemStateChangedEventTopic.replace("{memberName}", member.getName())
                .replace("{itemName}", groupItem.getName())));
        assertTrue(change.getItemState().equals(groupItem.getState()));
        assertTrue(change.getOldItemState().equals(oldGroupState));

        events.clear();

        // State doesn't change -> no events are fired
        member.setState(member.getState());
        assertThat(events.size(), is(0));
    }

    @Test
    public void assertThatGroupItemChangesRespectGroupFunctionOR() throws InterruptedException {
        events.clear();
        GroupItem groupItem = new GroupItem("root", new SwitchItem("mySwitch"),
                new ArithmeticGroupFunction.Or(OnOffType.ON, OnOffType.OFF));
        groupItem.setItemStateConverter(itemStateConverter);

        SwitchItem sw1 = new SwitchItem("switch1");
        SwitchItem sw2 = new SwitchItem("switch2");
        groupItem.addMember(sw1);
        groupItem.addMember(sw2);

        groupItem.setEventPublisher(publisher);

        // State changes -> one change event is fired
        sw1.setState(OnOffType.ON);

        waitForAssert(() -> assertThat(events, hasSize(1)));

        List<Event> groupItemStateChangedEvents = events.stream().filter(it -> it instanceof GroupItemStateChangedEvent)
                .collect(Collectors.toList());
        assertThat(groupItemStateChangedEvents, hasSize(1));

        GroupItemStateChangedEvent change = (GroupItemStateChangedEvent) groupItemStateChangedEvents.get(0);
        assertThat(change.getItemName(), is(groupItem.getName()));

        // we expect that the group should now have status "ON"
        assertThat(change.getOldItemState(), is(UnDefType.NULL));
        assertThat(change.getItemState(), is(OnOffType.ON));

        assertThat(groupItem.getState(), is(OnOffType.ON));

        events.clear();

        // State does not change -> no change event is fired
        sw2.setState(OnOffType.ON);

        // wait to see that the event doesn't fire
        Thread.sleep(WAIT_EVENT_TO_BE_HANDLED);

        waitForAssert(() -> assertThat(events, hasSize(0)));
    }

    @Test
    public void assertThatItemCommandEventsAreEmittedFromCommand() {
        events.clear();
        GroupItem groupItem = new GroupItem("root", new SwitchItem("mySwitch"),
                new ArithmeticGroupFunction.Or(OnOffType.ON, OnOffType.OFF));
        groupItem.setItemStateConverter(itemStateConverter);

        SwitchItem sw1 = new SwitchItem("switch1");
        SwitchItem sw2 = new SwitchItem("switch2");
        groupItem.addMember(sw1);
        groupItem.addMember(sw2);

        groupItem.setEventPublisher(publisher);

        // Command is sent to item -> no change event is fired
        groupItem.send(OnOffType.ON);

        waitForAssert(() -> assertThat(events, hasSize(2)));

        List<Event> itemCommandEvents = events.stream().filter(it -> it instanceof ItemCommandEvent)
                .collect(Collectors.toList());
        assertThat(itemCommandEvents, hasSize(2));

        List<Event> groupItemStateChangedEvents = events.stream().filter(it -> it instanceof GroupItemStateChangedEvent)
                .collect(Collectors.toList());
        assertThat(groupItemStateChangedEvents, hasSize(0));

        assertThat(groupItem.getState(), is(UnDefType.NULL));
    }

    @Test
    public void assertThatGroupItemChangesRespectGroupFunctionORWithUNDEF() throws InterruptedException {
        events.clear();
        GroupItem groupItem = new GroupItem("root", new SwitchItem("mySwitch"),
                new ArithmeticGroupFunction.Or(OnOffType.ON, OnOffType.OFF));
        groupItem.setItemStateConverter(itemStateConverter);

        SwitchItem sw1 = new SwitchItem("switch1");
        SwitchItem sw2 = new SwitchItem("switch2");
        groupItem.addMember(sw1);
        groupItem.addMember(sw2);

        groupItem.setEventPublisher(publisher);

        // State changes -> one change event is fired
        sw1.setState(OnOffType.ON);

        waitForAssert(() -> assertThat(events, hasSize(1)));

        List<Event> changes = events.stream().filter(it -> it instanceof GroupItemStateChangedEvent)
                .collect(Collectors.toList());
        assertThat(changes, hasSize(1));

        GroupItemStateChangedEvent change = (GroupItemStateChangedEvent) changes.get(0);
        assertTrue(change.getItemName().equals(groupItem.getName()));

        // we expect that the group should now have status "ON"
        assertTrue(change.getOldItemState().equals(UnDefType.NULL));
        assertTrue(change.getItemState().equals(OnOffType.ON));

        assertThat(groupItem.getState(), is(OnOffType.ON));

        events.clear();

        // State does not change -> no change event is fired
        sw2.setState(OnOffType.ON);

        sw2.setState(UnDefType.UNDEF);

        // wait to see that the event doesn't fire
        Thread.sleep(WAIT_EVENT_TO_BE_HANDLED);

        assertThat(events, hasSize(0));

        assertTrue(groupItem.getState().equals(OnOffType.ON));
    }

    @Test
    public void assertThatGroupItemChangesRespectGroupFunctionAND() {
        events.clear();
        GroupItem groupItem = new GroupItem("root", new SwitchItem("mySwitch"),
                new ArithmeticGroupFunction.And(OnOffType.ON, OnOffType.OFF));
        groupItem.setItemStateConverter(itemStateConverter);

        SwitchItem sw1 = new SwitchItem("switch1");
        SwitchItem sw2 = new SwitchItem("switch2");
        groupItem.addMember(sw1);
        groupItem.addMember(sw2);

        groupItem.setEventPublisher(publisher);

        // State changes -> one change event is fired
        sw1.setState(OnOffType.ON);

        waitForAssert(() -> assertThat(events, hasSize(1)));

        List<Event> changes = events.stream().filter(it -> it instanceof GroupItemStateChangedEvent)
                .collect(Collectors.toList());
        assertThat(changes, hasSize(1));

        GroupItemStateChangedEvent change = (GroupItemStateChangedEvent) changes.get(0);
        assertTrue(change.getItemName().equals(groupItem.getName()));

        // we expect that the group should now have status "OFF"
        assertTrue(change.getOldItemState().equals(UnDefType.NULL));
        assertTrue(change.getItemState().equals(OnOffType.OFF));

        assertThat(groupItem.getState(), is(OnOffType.OFF));

        events.clear();

        // State changes -> one change event is fired
        sw2.setState(OnOffType.ON);

        waitForAssert(() -> assertThat(events, hasSize(1)));

        changes = events.stream().filter(it -> it instanceof GroupItemStateChangedEvent).collect(Collectors.toList());
        assertThat(changes, hasSize(1));

        change = (GroupItemStateChangedEvent) changes.get(0);
        assertTrue(change.getItemName().equals(groupItem.getName()));

        // we expect that the group should now have status "ON"
        assertTrue(change.getOldItemState().equals(OnOffType.OFF));
        assertTrue(change.getItemState().equals(OnOffType.ON));

        assertTrue(groupItem.getState().equals(OnOffType.ON));
    }

    @SuppressWarnings("deprecation")
    @Test
    public void assertThatGroupItemChangesDoNotAffectTheGroupStatusIfnoFunctionOrBaseItemAreDefined()
            throws InterruptedException {
        events.clear();
        GroupItem groupItem = new GroupItem("root");

        TestItem member = new TestItem("member1");
        groupItem.addMember(member);
        groupItem.setEventPublisher(publisher);
        State oldGroupState = groupItem.getState();

        // State changes -> NO change event should be fired
        member.setState(new RawType());

        // wait to see that the event doesn't fire
        Thread.sleep(WAIT_EVENT_TO_BE_HANDLED);

        assertThat(events.size(), is(0));

        assertTrue(groupItem.getState().equals(oldGroupState));
    }

    @Test
    public void assertThatGroupItemWithoutFunctionCanHaveAconvertibleState() {
        GroupItem groupItem = new GroupItem("root");
        PercentType pt = new PercentType(50);
        groupItem.setState(pt);

        State groupStateAsOnOff = groupItem.getStateAs(OnOffType.class);

        // any value >0 means on, so 50% means the group state should be ON
        assertTrue(OnOffType.ON.equals(groupStateAsOnOff));
    }

    @Test
    public void assertThatGroupItemWithRollershutterBaseItemConversionWorks() {
        // initially this group has State UndefType.NULL
        GroupItem groupItem = new GroupItem("root", new RollershutterItem("myRollerShutter"));
        State groupStateAsOnOff = groupItem.getStateAs(OnOffType.class);

        // a state conversion from NULL to OnOffType should not be possible
        assertNull(groupStateAsOnOff);

        // init group
        groupItem.setState(new PercentType(70));
        groupStateAsOnOff = groupItem.getStateAs(OnOffType.class);

        // any value >0 means on, so 50% means the group state should be ON
        assertTrue(OnOffType.ON.equals(groupStateAsOnOff));
    }

    @Test
    public void assertThatGroupItemWithColoritemBaseItemConversionWorks() {
        // initially this group has State UndefType.NULL
        GroupItem groupItem = new GroupItem("root", new ColorItem("myColor"));
        State groupStateAsPercent = groupItem.getStateAs(PercentType.class);

        // a state conversion from NULL to PercentType should not be possible
        assertNull(groupStateAsPercent);

        // init group
        groupItem.setState(new HSBType("200,80,80"));
        groupStateAsPercent = groupItem.getStateAs(PercentType.class);

        assertTrue(groupStateAsPercent instanceof PercentType);
        assertThat(((PercentType) groupStateAsPercent).intValue(), is(80));
    }

    @Test
    public void assertThatGroupItemWithDimmeritemBaseItemConversionWorks() {
        // initially this group has State UndefType.NULL
        GroupItem groupItem = new GroupItem("root", new DimmerItem("myDimmer"));
        State groupStateAsPercent = groupItem.getStateAs(PercentType.class);

        // a state conversion from NULL to PercentType should not be possible
        assertNull(groupStateAsPercent);

        // init group
        groupItem.setState(new PercentType(80));
        groupStateAsPercent = groupItem.getStateAs(PercentType.class);

        assertTrue(groupStateAsPercent instanceof PercentType);
        assertThat(((PercentType) groupStateAsPercent).intValue(), is(80));
    }

    @Test
    public void assertThatGroupItemwithDimmeritemAcceptsGetsPercentTypeStateIfMembersHavePercentTypeStates() {
        events.clear();
        GroupItem groupItem = new GroupItem("root", new DimmerItem("myDimmer"), new ArithmeticGroupFunction.Avg());
        groupItem.setItemStateConverter(itemStateConverter);

        DimmerItem member1 = new DimmerItem("dimmer1");
        groupItem.addMember(member1);
        DimmerItem member2 = new DimmerItem("dimmer2");
        groupItem.addMember(member2);
        groupItem.setEventPublisher(publisher);

        member1.setState(new PercentType(50));

        waitForAssert(() -> assertThat(events.size(), is(1)));

        List<Event> changes = events.stream().filter(it -> it instanceof GroupItemStateChangedEvent)
                .collect(Collectors.toList());
        GroupItemStateChangedEvent change = (GroupItemStateChangedEvent) changes.get(0);
        assertTrue(change.getItemName().equals(groupItem.getName()));

        State newEventState = change.getItemState();
        assertTrue(newEventState instanceof PercentType);
        assertThat(((PercentType) newEventState).intValue(), is(50));

        State newGroupState = groupItem.getState();
        assertTrue(newGroupState instanceof PercentType);
        assertThat(((PercentType) newGroupState).intValue(), is(50));

        events.clear();

        member2.setState(new PercentType(10));

        waitForAssert(() -> assertThat(events.size(), is(1)));

        changes = events.stream().filter(it -> it instanceof GroupItemStateChangedEvent).collect(Collectors.toList());
        assertThat(changes.size(), is(1));

        change = (GroupItemStateChangedEvent) changes.get(0);
        assertTrue(change.getItemName().equals(groupItem.getName()));

        newEventState = change.getItemState();
        assertTrue(newEventState instanceof PercentType);
        assertThat(((PercentType) newEventState).intValue(), is(30));

        newGroupState = groupItem.getState();
        assertTrue(newGroupState instanceof PercentType);
        assertThat(((PercentType) newGroupState).intValue(), is(30));
    }

    @SuppressWarnings("null")
    @Test
    public void assertThatNumberGroupItemWithDimensionCalculatesCorrectState() {
        NumberItem baseItem = createNumberItem("baseItem", Temperature.class, UnDefType.NULL);
        GroupFunctionDTO gfDTO = new GroupFunctionDTO();
        gfDTO.name = "sum";
        GroupFunction function = groupFunctionHelper.createGroupFunction(gfDTO, Collections.emptyList(),
                Temperature.class);
        GroupItem groupItem = new GroupItem("number", baseItem, function);
        groupItem.setUnitProvider(unitProvider);

        NumberItem celsius = createNumberItem("C", Temperature.class, new QuantityType<>("23 °C"));
        groupItem.addMember(celsius);
        NumberItem fahrenheit = createNumberItem("F", Temperature.class, new QuantityType<>("23 °F"));
        groupItem.addMember(fahrenheit);
        NumberItem kelvin = createNumberItem("K", Temperature.class, new QuantityType<>("23 K"));
        groupItem.addMember(kelvin);

        QuantityType<?> state = groupItem.getStateAs(QuantityType.class);

        assertThat(state.getUnit(), is(Units.CELSIUS));
        assertThat(state.doubleValue(), is(-232.15d));

        celsius.setState(new QuantityType<>("265 °C"));

        state = groupItem.getStateAs(QuantityType.class);

        assertThat(state.getUnit(), is(Units.CELSIUS));
        assertThat(state.doubleValue(), is(9.85d));
    }

    @Test
    public void assertThatNumberGroupItemWithDifferentDimensionsCalculatesCorrectState() {
        NumberItem baseItem = createNumberItem("baseItem", Temperature.class, UnDefType.NULL);
        GroupFunctionDTO gfDTO = new GroupFunctionDTO();
        gfDTO.name = "sum";
        GroupFunction function = groupFunctionHelper.createGroupFunction(gfDTO, Collections.emptyList(),
                Temperature.class);
        GroupItem groupItem = new GroupItem("number", baseItem, function);
        groupItem.setUnitProvider(unitProvider);
        groupItem.setItemStateConverter(itemStateConverter);

        NumberItem celsius = createNumberItem("C", Temperature.class, new QuantityType<>("23 °C"));
        groupItem.addMember(celsius);
        NumberItem hectoPascal = createNumberItem("F", Pressure.class, new QuantityType<>("1010 hPa"));
        groupItem.addMember(hectoPascal);
        NumberItem percent = createNumberItem("K", Dimensionless.class, new QuantityType<>("110 %"));
        groupItem.addMember(percent);

        QuantityType<?> state = groupItem.getStateAs(QuantityType.class);

        assertThat(state, is(new QuantityType<>("23 °C")));

        groupItem.stateUpdated(celsius, UnDefType.NULL);
        assertThat(groupItem.getState(), is(new QuantityType<>("23 °C")));
    }

    private NumberItem createNumberItem(String name, Class<? extends Quantity<?>> dimension, State state) {
        NumberItem item = new NumberItem(CoreItemFactory.NUMBER + ":" + dimension.getSimpleName(), name);
        item.setUnitProvider(unitProvider);
        item.setState(state);

        return item;
    }

}
