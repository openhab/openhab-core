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
package org.openhab.core.io.rest.core.item;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.net.URI;

import org.junit.Before;
import org.junit.Test;
import org.openhab.core.items.GenericItem;
import org.openhab.core.items.GroupItem;
import org.openhab.core.library.CoreItemFactory;
import org.openhab.core.test.java.JavaTest;

/**
 * @author Kai Kreuzer - Initial contribution
 */
public class EnrichedItemDTOMapperTest extends JavaTest {

    private CoreItemFactory itemFactory;

    @Before
    public void setup() {
        itemFactory = new CoreItemFactory();
    }

    @Test
    public void testFiltering() {
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

        EnrichedGroupItemDTO dto = (EnrichedGroupItemDTO) EnrichedItemDTOMapper.map(group, false, null, URI.create(""),
                null);
        assertThat(dto.members.length, is(0));

        dto = (EnrichedGroupItemDTO) EnrichedItemDTOMapper.map(group, true, null, URI.create(""), null);
        assertThat(dto.members.length, is(3));
        assertThat(((EnrichedGroupItemDTO) dto.members[0]).members.length, is(1));

        dto = (EnrichedGroupItemDTO) EnrichedItemDTOMapper.map(group, true,
                i -> CoreItemFactory.NUMBER.equals(i.getType()), URI.create(""), null);
        assertThat(dto.members.length, is(1));

        dto = (EnrichedGroupItemDTO) EnrichedItemDTOMapper.map(group, true,
                i -> CoreItemFactory.NUMBER.equals(i.getType()) || i instanceof GroupItem, URI.create(""), null);
        assertThat(dto.members.length, is(2));
        assertThat(((EnrichedGroupItemDTO) dto.members[0]).members.length, is(0));

        dto = (EnrichedGroupItemDTO) EnrichedItemDTOMapper.map(group, true,
                i -> CoreItemFactory.NUMBER.equals(i.getType()) || i instanceof GroupItem, URI.create(""), null);
        assertThat(dto.members.length, is(2));
        assertThat(((EnrichedGroupItemDTO) dto.members[0]).members.length, is(0));

        dto = (EnrichedGroupItemDTO) EnrichedItemDTOMapper.map(group, true,
                i -> CoreItemFactory.NUMBER.equals(i.getType()) || i.getType().equals(CoreItemFactory.STRING)
                        || i instanceof GroupItem,
                URI.create(""), null);
        assertThat(dto.members.length, is(2));
        assertThat(((EnrichedGroupItemDTO) dto.members[0]).members.length, is(1));
    }

}
