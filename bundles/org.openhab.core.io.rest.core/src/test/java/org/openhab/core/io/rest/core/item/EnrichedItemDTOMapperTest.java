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
package org.openhab.core.io.rest.core.item;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openhab.core.items.GenericItem;
import org.openhab.core.items.GroupItem;
import org.openhab.core.library.CoreItemFactory;
import org.openhab.core.library.items.NumberItem;
import org.openhab.core.test.java.JavaTest;

/**
 * @author Kai Kreuzer - Initial contribution
 */
@NonNullByDefault
public class EnrichedItemDTOMapperTest extends JavaTest {

    @BeforeEach
    public void setup() {
        setupInterceptedLogger(EnrichedItemDTOMapper.class, LogLevel.DEBUG);
    }

    @Test
    public void testFiltering() {
        CoreItemFactory itemFactory = new CoreItemFactory();

        GroupItem group = new GroupItem("TestGroup");
        GroupItem subGroup = new GroupItem("TestSubGroup");
        GenericItem switchItem = itemFactory.createItem(CoreItemFactory.SWITCH, "TestSwitch");
        GenericItem numberItem = itemFactory.createItem(CoreItemFactory.NUMBER, "TestNumber");
        GenericItem stringItem = itemFactory.createItem(CoreItemFactory.STRING, "TestString");

        if (switchItem != null && numberItem != null && stringItem != null) {
            group.addMember(subGroup);
            group.addMember(switchItem);
            group.addMember(numberItem);
            subGroup.addMember(stringItem);
        }

        EnrichedGroupItemDTO dto = (EnrichedGroupItemDTO) EnrichedItemDTOMapper.map(group, false, null, null, null);
        assertThat(dto.members.length, is(0));

        dto = (EnrichedGroupItemDTO) EnrichedItemDTOMapper.map(group, true, null, null, null);
        assertThat(dto.members.length, is(3));
        assertThat(((EnrichedGroupItemDTO) dto.members[0]).members.length, is(1));

        dto = (EnrichedGroupItemDTO) EnrichedItemDTOMapper.map(group, true,
                i -> CoreItemFactory.NUMBER.equals(i.getType()), null, null);
        assertThat(dto.members.length, is(1));

        dto = (EnrichedGroupItemDTO) EnrichedItemDTOMapper.map(group, true,
                i -> CoreItemFactory.NUMBER.equals(i.getType()) || i instanceof GroupItem, null, null);
        assertThat(dto.members.length, is(2));
        assertThat(((EnrichedGroupItemDTO) dto.members[0]).members.length, is(0));

        dto = (EnrichedGroupItemDTO) EnrichedItemDTOMapper.map(group, true,
                i -> CoreItemFactory.NUMBER.equals(i.getType()) || i instanceof GroupItem, null, null);
        assertThat(dto.members.length, is(2));
        assertThat(((EnrichedGroupItemDTO) dto.members[0]).members.length, is(0));

        dto = (EnrichedGroupItemDTO) EnrichedItemDTOMapper.map(group, true,
                i -> CoreItemFactory.NUMBER.equals(i.getType()) || i.getType().equals(CoreItemFactory.STRING)
                        || i instanceof GroupItem,
                null, null);
        assertThat(dto.members.length, is(2));
        assertThat(((EnrichedGroupItemDTO) dto.members[0]).members.length, is(1));
    }

    @Test
    public void testDirectRecursiveMembershipDoesNotThrowStackOverflowException() {
        GroupItem groupItem1 = new GroupItem("group1");
        GroupItem groupItem2 = new GroupItem("group2");

        groupItem1.addMember(groupItem2);
        groupItem2.addMember(groupItem1);

        assertDoesNotThrow(() -> EnrichedItemDTOMapper.map(groupItem1, true, null, null, null));

        assertLogMessage(EnrichedItemDTOMapper.class, LogLevel.ERROR,
                "Recursive group membership found: group1 is both, a direct or indirect parent and a child of group2.");
    }

    @Test
    public void testIndirectRecursiveMembershipDoesNotThrowStackOverflowException() {
        GroupItem groupItem1 = new GroupItem("group1");
        GroupItem groupItem2 = new GroupItem("group2");
        GroupItem groupItem3 = new GroupItem("group3");

        groupItem1.addMember(groupItem2);
        groupItem2.addMember(groupItem3);
        groupItem3.addMember(groupItem1);

        assertDoesNotThrow(() -> EnrichedItemDTOMapper.map(groupItem1, true, null, null, null));

        assertLogMessage(EnrichedItemDTOMapper.class, LogLevel.ERROR,
                "Recursive group membership found: group1 is both, a direct or indirect parent and a child of group3.");
    }

    @Test
    public void testDuplicateMembershipOfPlainItemsDoesNotTriggerWarning() {
        GroupItem groupItem1 = new GroupItem("group1");
        GroupItem groupItem2 = new GroupItem("group2");
        NumberItem numberItem = new NumberItem("number");

        groupItem1.addMember(groupItem2);
        groupItem1.addMember(numberItem);
        groupItem2.addMember(numberItem);

        EnrichedItemDTOMapper.map(groupItem1, true, null, null, null);

        assertNoLogMessage(EnrichedItemDTOMapper.class);
    }
}
