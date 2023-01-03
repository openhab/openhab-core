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
package org.openhab.core.thing.type;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openhab.core.thing.CommonTriggerEvents;
import org.openhab.core.types.EventDescription;
import org.openhab.core.types.EventOption;
import org.openhab.core.types.StateDescription;
import org.openhab.core.types.StateDescriptionFragment;
import org.openhab.core.types.StateDescriptionFragmentBuilder;

/**
 * Tests the {@link ChannelTypeBuilder}.
 *
 * @author Stefan Triller - Initial contribution
 */
@NonNullByDefault
public class ChannelTypeBuilderTest {

    private static final String DESCRIPTION = "description";
    private static final String ITEM_TYPE = "itemType";
    private static final String CATEGORY = "category";
    private static final String LABEL = "label";
    private static final String TAG = "tag";
    private static final List<String> TAGS = List.of("TAG1", "TAG2");
    private static final URI CONFIG_DESCRIPTION_URL = URI.create("config:dummy");
    private static final ChannelTypeUID CHANNEL_TYPE_UID = new ChannelTypeUID("bindingId", "channelId");
    private static final StateDescriptionFragment STATE_DESCRIPTION_FRAGMENT = StateDescriptionFragmentBuilder.create()
            .withMinimum(BigDecimal.ZERO).withMaximum(new BigDecimal(100)).withStep(BigDecimal.ONE).withPattern("%s")
            .build();
    private static final StateDescription STATE_DESCRIPTION = Objects
            .requireNonNull(STATE_DESCRIPTION_FRAGMENT.toStateDescription());
    private static final EventDescription EVENT_DESCRIPTION = new EventDescription(
            List.of(new EventOption(CommonTriggerEvents.DIR1_PRESSED, null),
                    new EventOption(CommonTriggerEvents.DIR1_RELEASED, null)));

    private @NonNullByDefault({}) StateChannelTypeBuilder stateBuilder;
    private @NonNullByDefault({}) TriggerChannelTypeBuilder triggerBuilder;

    @BeforeEach
    public void setup() {
        // set up a valid basic ChannelTypeBuilder
        stateBuilder = ChannelTypeBuilder.state(CHANNEL_TYPE_UID, LABEL, ITEM_TYPE);
        triggerBuilder = ChannelTypeBuilder.trigger(CHANNEL_TYPE_UID, LABEL);
    }

    @Test
    public void whenLabelIsBlankForStateShouldFail() {
        assertThrows(IllegalArgumentException.class, () -> ChannelTypeBuilder.state(CHANNEL_TYPE_UID, "", ITEM_TYPE));
    }

    @Test
    public void whenItemTypeIsBlankForStateShouldFail() {
        assertThrows(IllegalArgumentException.class, () -> ChannelTypeBuilder.state(CHANNEL_TYPE_UID, LABEL, ""));
    }

    @Test
    public void whenLabelIsBlankForTriggerShouldFail() {
        assertThrows(IllegalArgumentException.class, () -> ChannelTypeBuilder.trigger(CHANNEL_TYPE_UID, ""));
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
        ChannelType channelType = stateBuilder.withConfigDescriptionURI(CONFIG_DESCRIPTION_URL).build();

        assertThat(channelType.getConfigDescriptionURI(), is(CONFIG_DESCRIPTION_URL));
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
    public void withStateDescriptionFragmentShouldSetStateDescription() {
        ChannelType channelType = stateBuilder.withStateDescriptionFragment(STATE_DESCRIPTION_FRAGMENT).build();

        assertThat(channelType.getState(), is(STATE_DESCRIPTION));
    }

    @Test
    public void withEventDescriptionShouldSetEventDescription() {
        ChannelType channelType = triggerBuilder.withEventDescription(EVENT_DESCRIPTION).build();

        assertThat(channelType.getEvent(), is(EVENT_DESCRIPTION));
        assertThat(channelType.getKind(), is(ChannelKind.TRIGGER));
    }
}
