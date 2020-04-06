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
package org.openhab.core.thing.type;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

/**
 * Tests the {@link ChannelGroupTypeBuilder}.
 *
 * @author Christoph Weitkamp - Initial contribution
 */
public class ChannelGroupTypeBuilderTest {

    private static final String DESCRIPTION = "description";
    private static final String CATEGORY = "category";
    private static final String LABEL = "label";
    private static final ChannelGroupTypeUID CHANNEL_GROUP_TYPE_UID = new ChannelGroupTypeUID("bindingId",
            "channelGroupId");

    private ChannelGroupTypeBuilder builder;

    @Before
    public void setup() {
        // set up a valid basic ChannelGroupTypeBuilder
        builder = ChannelGroupTypeBuilder.instance(CHANNEL_GROUP_TYPE_UID, LABEL);
    }

    @Test(expected = IllegalArgumentException.class)
    public void whenLabelIsBlankForStateShouldFail() {
        ChannelGroupTypeBuilder.instance(CHANNEL_GROUP_TYPE_UID, "");
    }

    @Test
    public void withLabelAndChannelGroupTypeUIDShouldCreateChannelGroupType() {
        ChannelGroupType channelGroupType = builder.build();

        assertEquals(CHANNEL_GROUP_TYPE_UID, channelGroupType.getUID());
        assertEquals(LABEL, channelGroupType.getLabel());
    }

    @Test
    public void withDescriptionShouldSetDescription() {
        ChannelGroupType channelGroupType = builder.withDescription(DESCRIPTION).build();

        assertEquals(DESCRIPTION, channelGroupType.getDescription());
    }

    @Test
    public void withCategoryShouldSetCategory() {
        ChannelGroupType channelGroupType = builder.withCategory(CATEGORY).build();

        assertEquals(CATEGORY, channelGroupType.getCategory());
    }

    @Test
    public void withChannelDefinitionsShouldSetUnmodifiableChannelDefinitions() {
        ChannelGroupType channelGroupType = builder.withChannelDefinitions(mockList(ChannelDefinition.class, 2))
                .build();

        assertEquals(2, channelGroupType.getChannelDefinitions().size());
        try {
            channelGroupType.getChannelDefinitions().add(mock(ChannelDefinition.class));
            fail();
        } catch (UnsupportedOperationException e) {
            // expected
        }
    }

    private <T> List<T> mockList(Class<T> entityClass, int size) {
        List<T> result = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            result.add(mock(entityClass));
        }
        return result;
    }
}
