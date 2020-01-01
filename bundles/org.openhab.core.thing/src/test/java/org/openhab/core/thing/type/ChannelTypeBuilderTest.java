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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.openhab.core.thing.CommonTriggerEvents;
import org.openhab.core.types.EventDescription;
import org.openhab.core.types.EventOption;
import org.openhab.core.types.StateDescription;

/**
 * Tests the {@link ChannelTypeBuilder}.
 *
 * @author Stefan Triller - Initial contribution
 */
public class ChannelTypeBuilderTest {

    private static final String DESCRIPTION = "description";
    private static final String ITEM_TYPE = "itemType";
    private static final String CATEGORY = "category";
    private static final String LABEL = "label";
    private static final String TAG = "tag";
    private static final List<String> TAGS = Arrays.asList("TAG1", "TAG2");
    private static URI configDescriptionUri;
    private static final ChannelTypeUID CHANNEL_TYPE_UID = new ChannelTypeUID("bindingId", "channelId");
    private static final StateDescription STATE_DESCRIPTION = new StateDescription(BigDecimal.ZERO, new BigDecimal(100),
            BigDecimal.ONE, "%s", false, null);
    private static final EventDescription EVENT_DESCRIPTION = new EventDescription(
            Arrays.asList(new EventOption(CommonTriggerEvents.DIR1_PRESSED, null),
                    new EventOption(CommonTriggerEvents.DIR1_RELEASED, null)));

    private StateChannelTypeBuilder stateBuilder;
    private TriggerChannelTypeBuilder triggerBuilder;

    @Before
    public void setup() throws URISyntaxException {
        configDescriptionUri = new URI("config:dummy");
        // set up a valid basic ChannelTypeBuilder
        stateBuilder = ChannelTypeBuilder.state(CHANNEL_TYPE_UID, LABEL, ITEM_TYPE);
        triggerBuilder = ChannelTypeBuilder.trigger(CHANNEL_TYPE_UID, LABEL);
    }

    @Test(expected = IllegalArgumentException.class)
    public void whenLabelIsBlankForStateShouldFail() {
        ChannelTypeBuilder.state(CHANNEL_TYPE_UID, "", ITEM_TYPE);
    }

    @Test(expected = IllegalArgumentException.class)
    public void whenItemTypeIsBlankForStateShouldFail() {
        ChannelTypeBuilder.state(CHANNEL_TYPE_UID, LABEL, "");
    }

    @Test(expected = IllegalArgumentException.class)
    public void whenLabelIsBlankForTriggerShouldFail() {
        ChannelTypeBuilder.trigger(CHANNEL_TYPE_UID, "");
    }

    @Test
    public void withLabelAndChannelTypeUIDShouldCreateChannelType() {
        ChannelType channelType = stateBuilder.build();

        assertThat(channelType.getUID(), is(CHANNEL_TYPE_UID));
        assertThat(channelType.getItemType(), is(ITEM_TYPE));
        assertThat(channelType.getLabel(), is(LABEL));
        assertThat(channelType.getKind(), is(ChannelKind.STATE));
    }

    @Test
    public void withDefaultAdvancedIsFalse() {
        ChannelType channelType = stateBuilder.build();

        assertThat(channelType.isAdvanced(), is(false));
    }

    @Test
    public void isAdvancedShouldSetAdvanced() {
        ChannelType channelType = stateBuilder.isAdvanced(true).build();

        assertThat(channelType.isAdvanced(), is(true));
    }

    @Test
    public void withDescriptionShouldSetDescription() {
        ChannelType channelType = stateBuilder.withDescription(DESCRIPTION).build();

        assertThat(channelType.getDescription(), is(DESCRIPTION));
    }

    @Test
    public void withCategoryShouldSetCategory() {
        ChannelType channelType = stateBuilder.withCategory(CATEGORY).build();

        assertThat(channelType.getCategory(), is(CATEGORY));
    }

    @Test
    public void withConfigDescriptionURIShouldSetConfigDescriptionURI() {
        ChannelType channelType = stateBuilder.withConfigDescriptionURI(configDescriptionUri).build();

        assertThat(channelType.getConfigDescriptionURI(), is(configDescriptionUri));
    }

    @Test
    public void withTagsShouldSetTag() {
        ChannelType channelType = stateBuilder.withTag(TAG).build();

        assertThat(channelType.getTags(), is(hasSize(1)));
    }

    @Test
    public void withTagsShouldSetTags() {
        ChannelType channelType = stateBuilder.withTags(TAGS).build();

        assertThat(channelType.getTags(), is(hasSize(2)));
    }

    @Test
    public void withStateDescriptionShouldSetStateDescription() {
        ChannelType channelType = stateBuilder.withStateDescription(STATE_DESCRIPTION).build();

        assertThat(channelType.getState(), is(STATE_DESCRIPTION));
    }

    @Test
    public void withEventDescriptionShouldSetEventDescription() {
        ChannelType channelType = triggerBuilder.withEventDescription(EVENT_DESCRIPTION).build();

        assertThat(channelType.getEvent(), is(EVENT_DESCRIPTION));
        assertThat(channelType.getKind(), is(ChannelKind.TRIGGER));
    }
}
