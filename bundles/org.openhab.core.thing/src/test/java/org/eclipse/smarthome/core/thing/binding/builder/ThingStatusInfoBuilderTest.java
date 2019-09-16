/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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
package org.eclipse.smarthome.core.thing.binding.builder;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingStatusInfo;
import org.junit.Before;
import org.junit.Test;

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
        ThingStatusInfo thigStatusInfo = builder.build();

        assertThat(thigStatusInfo.getStatus(), is(ThingStatus.ONLINE));
        assertThat(thigStatusInfo.getStatusDetail(), is(ThingStatusDetail.NONE));
        assertThat(thigStatusInfo.getDescription(), is(nullValue()));
    }

    @Test
    public void testThingStatusInfoBuilderStatusDetails() {
        ThingStatusInfo thigStatusInfo = builder.withStatusDetail(ThingStatusDetail.DISABLED).build();

        assertThat(thigStatusInfo.getStatus(), is(ThingStatus.ONLINE));
        assertThat(thigStatusInfo.getStatusDetail(), is(ThingStatusDetail.DISABLED));
        assertThat(thigStatusInfo.getDescription(), is(nullValue()));
    }

    @Test
    public void testThingStatusInfoBuilderStatusDescription() {
        ThingStatusInfo thigStatusInfo = builder.withDescription("My test description").build();

        assertThat(thigStatusInfo.getStatus(), is(ThingStatus.ONLINE));
        assertThat(thigStatusInfo.getStatusDetail(), is(ThingStatusDetail.NONE));
        assertThat(thigStatusInfo.getDescription(), is("My test description"));
    }

    @Test
    public void subsequentBuildsCreateIndependentThingStatusInfos() {
        ThingStatusInfo thigStatusInfo1 = builder.build();
        ThingStatusInfo thigStatusInfo2 = builder.withStatusDetail(ThingStatusDetail.DISABLED)
                .withDescription("My test description").build();

        assertThat(thigStatusInfo2.getStatus(), is(thigStatusInfo1.getStatus()));
        assertThat(thigStatusInfo2.getStatusDetail(), is(not(thigStatusInfo1.getStatusDetail())));
        assertThat(thigStatusInfo2.getDescription(), is(not(thigStatusInfo1.getDescription())));
    }

}
