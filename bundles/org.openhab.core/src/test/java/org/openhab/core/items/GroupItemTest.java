/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.openhab.core.library.items.DimmerItem;
import org.openhab.core.library.items.NumberItem;
import org.openhab.core.library.items.StringItem;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.types.UnDefType;

/**
 * @author Christoph Weitkamp - Initial contribution
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
class GroupItemTest {

    private @Mock MetadataRegistry metadataRegistry;

    @Test
    public void testInvalidMetadataConfiguration() {
        GroupItem rootGroupItem = new GroupItem("root");
        rootGroupItem.setMetadataRegistry(metadataRegistry);

        MetadataKey key = new MetadataKey("sortBy", "root");
        when(metadataRegistry.get(key)).thenReturn(new Metadata(key, "INVALID", Map.of()));

        TestItem member3 = new TestItem("member3");
        rootGroupItem.addMember(member3);

        TestItem member1 = new TestItem("member1");
        rootGroupItem.addMember(member1);

        TestItem member2 = new TestItem("member2");
        rootGroupItem.addMember(member2);

        List<Item> members = rootGroupItem.getSortedMembers();
        assertThat(members, hasSize(3));

        List<Item> expected = List.of(member3, member1, member2);
        for (int i = 0; i < members.size(); i++) {
            assertEquals(expected.get(i).getName(), members.get(i).getName());
        }
    }

    @Test
    public void testGetSortedMembersByNameAscending() {
        GroupItem rootGroupItem = new GroupItem("root");
        rootGroupItem.setMetadataRegistry(metadataRegistry);

        MetadataKey key = new MetadataKey("sortBy", "root");
        when(metadataRegistry.get(key)).thenReturn(new Metadata(key, "NAME", Map.of()));

        TestItem member3 = new TestItem("member3");
        rootGroupItem.addMember(member3);

        TestItem member1 = new TestItem("member1");
        rootGroupItem.addMember(member1);

        TestItem member2 = new TestItem("member2");
        rootGroupItem.addMember(member2);

        List<Item> members = rootGroupItem.getSortedMembers();
        assertThat(members, hasSize(3));

        List<Item> expected = List.of(member1, member2, member3);
        for (int i = 0; i < members.size(); i++) {
            assertEquals(expected.get(i).getName(), members.get(i).getName());
        }
    }

    @Test
    public void testGetSortedMembersByNameDescending() {
        GroupItem rootGroupItem = new GroupItem("root");
        rootGroupItem.setMetadataRegistry(metadataRegistry);

        MetadataKey key = new MetadataKey("sortBy", "root");
        when(metadataRegistry.get(key)).thenReturn(new Metadata(key, "NAME", Map.of("ordering", "DESCENDING")));

        TestItem member3 = new TestItem("member3");
        rootGroupItem.addMember(member3);

        TestItem member1 = new TestItem("member1");
        rootGroupItem.addMember(member1);

        TestItem member2 = new TestItem("member2");
        rootGroupItem.addMember(member2);

        List<Item> members = rootGroupItem.getSortedMembers();
        assertThat(members, hasSize(3));

        List<Item> expected = List.of(member3, member2, member1);
        for (int i = 0; i < members.size(); i++) {
            assertEquals(expected.get(i).getName(), members.get(i).getName());
        }
    }

    @Test
    public void testGetSortedMembersByLabelAscending() {
        GroupItem rootGroupItem = new GroupItem("root");
        rootGroupItem.setMetadataRegistry(metadataRegistry);

        MetadataKey key = new MetadataKey("sortBy", "root");
        when(metadataRegistry.get(key)).thenReturn(new Metadata(key, "LABEL", Map.of("ordering", "ASCENDING")));

        TestItem member1 = new TestItem("member1");
        member1.setLabel("C");
        rootGroupItem.addMember(member1);

        TestItem member2 = new TestItem("member2");
        member2.setLabel("A");
        rootGroupItem.addMember(member2);

        TestItem member3 = new TestItem("member3");
        member3.setLabel("B");
        rootGroupItem.addMember(member3);

        List<Item> members = rootGroupItem.getSortedMembers();
        assertThat(members, hasSize(3));

        List<Item> expected = List.of(member2, member3, member1);
        for (int i = 0; i < members.size(); i++) {
            assertEquals(expected.get(i).getName(), members.get(i).getName());
        }
    }

    @Test
    public void testGetSortedMembersByStringTypeStateAscending() {
        GroupItem rootGroupItem = new GroupItem("root", new StringItem("baseItem"));
        rootGroupItem.setMetadataRegistry(metadataRegistry);

        MetadataKey key = new MetadataKey("sortBy", "root");
        when(metadataRegistry.get(key)).thenReturn(new Metadata(key, "STATE", Map.of("ordering", "ASCENDING")));

        StringItem member1 = new StringItem("member1");
        member1.setState(new StringType("C"));
        rootGroupItem.addMember(member1);

        StringItem member2 = new StringItem("member2");
        member2.setState(new StringType("A"));
        rootGroupItem.addMember(member2);

        StringItem member3 = new StringItem("member3");
        member3.setState(new StringType("B"));
        rootGroupItem.addMember(member3);

        List<Item> members = rootGroupItem.getSortedMembers();
        assertThat(members, hasSize(3));

        List<Item> expected = List.of(member2, member3, member1);
        for (int i = 0; i < members.size(); i++) {
            assertEquals(expected.get(i).getName(), members.get(i).getName());
        }
    }

    @Test
    public void testGetSortedMembersWithoutBaseItemAscending() {
        GroupItem rootGroupItem = new GroupItem("root");
        rootGroupItem.setMetadataRegistry(metadataRegistry);

        MetadataKey key = new MetadataKey("sortBy", "root");
        when(metadataRegistry.get(key)).thenReturn(new Metadata(key, "STATE", Map.of("ordering", "ASCENDING")));

        NumberItem member1 = new NumberItem("member1");
        member1.setState(DecimalType.valueOf("1"));
        rootGroupItem.addMember(member1);

        NumberItem member2 = new NumberItem("member2");
        member2.setState(new QuantityType<>("2 °C"));
        rootGroupItem.addMember(member2);

        NumberItem member3 = new NumberItem("member3");
        member3.setState(DecimalType.valueOf("0.5"));
        rootGroupItem.addMember(member3);

        NumberItem member4 = new NumberItem("member4");
        member4.setState(DecimalType.valueOf("-1"));
        rootGroupItem.addMember(member4);

        NumberItem member5 = new NumberItem("member5");
        member5.setState(UnDefType.UNDEF);
        rootGroupItem.addMember(member5);

        List<Item> members = rootGroupItem.getSortedMembers();
        assertThat(members, hasSize(5));

        List<Item> expected = List.of(member4, member3, member1, member2, member5);
        for (int i = 0; i < members.size(); i++) {
            assertEquals(expected.get(i).getName(), members.get(i).getName());
        }
    }

    @Test
    public void testGetSortedMembersByDecimalTypeStateAscending() {
        GroupItem rootGroupItem = new GroupItem("root", new NumberItem("baseItem"));
        rootGroupItem.setMetadataRegistry(metadataRegistry);

        MetadataKey key = new MetadataKey("sortBy", "root");
        when(metadataRegistry.get(key)).thenReturn(new Metadata(key, "STATE", Map.of("ordering", "ASCENDING")));

        NumberItem member1 = new NumberItem("member1");
        member1.setState(DecimalType.valueOf("1"));
        rootGroupItem.addMember(member1);

        NumberItem member2 = new NumberItem("member2");
        member2.setState(new QuantityType<>("2 °C"));
        rootGroupItem.addMember(member2);

        NumberItem member3 = new NumberItem("member3");
        member3.setState(DecimalType.valueOf("0.5"));
        rootGroupItem.addMember(member3);

        NumberItem member4 = new NumberItem("member4");
        member4.setState(DecimalType.valueOf("-1"));
        rootGroupItem.addMember(member4);

        NumberItem member5 = new NumberItem("member5");
        member5.setState(UnDefType.UNDEF);
        rootGroupItem.addMember(member5);

        List<Item> members = rootGroupItem.getSortedMembers();
        assertThat(members, hasSize(5));

        List<Item> expected = List.of(member5, member4, member3, member1, member2);
        for (int i = 0; i < members.size(); i++) {
            assertEquals(expected.get(i).getName(), members.get(i).getName());
        }
    }

    @Test
    public void testGetSortedMembersByDecimalTypeStateDescending() {
        GroupItem rootGroupItem = new GroupItem("root", new NumberItem("baseItem"));
        rootGroupItem.setMetadataRegistry(metadataRegistry);

        MetadataKey key = new MetadataKey("sortBy", "root");
        when(metadataRegistry.get(key)).thenReturn(new Metadata(key, "STATE", Map.of("ordering", "DESCENDING")));

        NumberItem member1 = new NumberItem("member1");
        member1.setState(DecimalType.valueOf("1"));
        rootGroupItem.addMember(member1);

        NumberItem member2 = new NumberItem("member2");
        member2.setState(new QuantityType<>("2 °C"));
        rootGroupItem.addMember(member2);

        NumberItem member3 = new NumberItem("member3");
        member3.setState(DecimalType.valueOf("0.5"));
        rootGroupItem.addMember(member3);

        NumberItem member4 = new NumberItem("member4");
        member4.setState(DecimalType.valueOf("-1"));
        rootGroupItem.addMember(member4);

        NumberItem member5 = new NumberItem("member5");
        member5.setState(UnDefType.UNDEF);
        rootGroupItem.addMember(member5);

        List<Item> members = rootGroupItem.getSortedMembers();
        assertThat(members, hasSize(5));

        List<Item> expected = List.of(member2, member1, member3, member4, member5);
        for (int i = 0; i < members.size(); i++) {
            assertEquals(expected.get(i).getName(), members.get(i).getName());
        }
    }

    @Test
    public void testGetSortedMembersByPercentTypeStateAscending() {
        GroupItem rootGroupItem = new GroupItem("root", new DimmerItem("baseItem"));
        rootGroupItem.setMetadataRegistry(metadataRegistry);

        MetadataKey key = new MetadataKey("sortBy", "root");
        when(metadataRegistry.get(key)).thenReturn(new Metadata(key, "STATE", Map.of("ordering", "ASCENDING")));

        DimmerItem member1 = new DimmerItem("member1");
        member1.setState(PercentType.valueOf("1"));
        rootGroupItem.addMember(member1);

        DimmerItem member2 = new DimmerItem("member2");
        member2.setState(PercentType.HUNDRED);
        rootGroupItem.addMember(member2);

        DimmerItem member3 = new DimmerItem("member3");
        member3.setState(PercentType.ZERO);
        rootGroupItem.addMember(member3);

        DimmerItem member4 = new DimmerItem("member4");
        member4.setState(OnOffType.ON);
        rootGroupItem.addMember(member4);

        DimmerItem member5 = new DimmerItem("member5");
        member5.setState(UnDefType.UNDEF);
        rootGroupItem.addMember(member5);

        List<Item> members = rootGroupItem.getSortedMembers();
        assertThat(members, hasSize(5));

        List<Item> expected = List.of(member5, member3, member1, member2, member4);
        for (int i = 0; i < members.size(); i++) {
            assertEquals(expected.get(i).getName(), members.get(i).getName());
        }
    }
}
