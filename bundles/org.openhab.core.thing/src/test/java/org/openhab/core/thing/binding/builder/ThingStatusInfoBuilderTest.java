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
package org.openhab.core.thing.binding.builder;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.ThingStatusInfo;

/**
 * Tests the {@link ThingStatusInfoBuilder}.
 *
 * @author Christoph Weitkamp - Initial contribution
 */
public class ThingStatusInfoBuilderTest {

    private ThingStatusInfoBuilder builder;

    @Before
    public void setup() {
        builder = ThingStatusInfoBuilder.create(ThingStatus.ONLINE);
    }

    @Test
    public void testThingStatusInfoBuilderStatus() {
        ThingStatusInfo thingStatusInfo = builder.build();

        assertThat(thingStatusInfo.getStatus(), is(ThingStatus.ONLINE));
        assertThat(thingStatusInfo.getStatusDetail(), is(ThingStatusDetail.NONE));
        assertThat(thingStatusInfo.getDescription(), is(nullValue()));
    }

    @Test
    public void testThingStatusInfoBuilderStatusDetails() {
        ThingStatusInfo thingStatusInfo = builder.withStatusDetail(ThingStatusDetail.DISABLED).build();

        assertThat(thingStatusInfo.getStatus(), is(ThingStatus.ONLINE));
        assertThat(thingStatusInfo.getStatusDetail(), is(ThingStatusDetail.DISABLED));
        assertThat(thingStatusInfo.getDescription(), is(nullValue()));
    }

    @Test
    public void testThingStatusInfoBuilderStatusDescription() {
        ThingStatusInfo thingStatusInfo = builder.withDescription("My test description").build();

        assertThat(thingStatusInfo.getStatus(), is(ThingStatus.ONLINE));
        assertThat(thingStatusInfo.getStatusDetail(), is(ThingStatusDetail.NONE));
        assertThat(thingStatusInfo.getDescription(), is("My test description"));
    }

    @Test
    public void subsequentBuildsCreateIndependentThingStatusInfos() {
        ThingStatusInfo thingStatusInfo1 = builder.build();
        ThingStatusInfo thingStatusInfo2 = builder.withStatusDetail(ThingStatusDetail.DISABLED)
                .withDescription("My test description").build();

        assertThat(thingStatusInfo2.getStatus(), is(thingStatusInfo1.getStatus()));
        assertThat(thingStatusInfo2.getStatusDetail(), is(not(thingStatusInfo1.getStatusDetail())));
        assertThat(thingStatusInfo2.getDescription(), is(not(thingStatusInfo1.getDescription())));
    }
}
