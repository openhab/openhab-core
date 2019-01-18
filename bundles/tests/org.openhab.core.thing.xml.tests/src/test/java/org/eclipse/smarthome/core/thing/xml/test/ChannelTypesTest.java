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
package org.eclipse.smarthome.core.thing.xml.test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

import java.util.Collection;

import org.eclipse.smarthome.core.thing.type.ChannelGroupType;
import org.eclipse.smarthome.core.thing.type.ChannelGroupTypeProvider;
import org.eclipse.smarthome.core.thing.type.ChannelType;
import org.eclipse.smarthome.core.thing.type.ChannelTypeProvider;
import org.eclipse.smarthome.test.SyntheticBundleInstaller;
import org.eclipse.smarthome.test.java.JavaOSGiTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.Bundle;

/***
 *
 * This test checks if channel types are loaded properly.
 *
 * @author Dennis Nobel - Initial contribution
 */
public class ChannelTypesTest extends JavaOSGiTest {

    private static final String TEST_BUNDLE_NAME = "ChannelTypesTest.bundle";

    private ChannelTypeProvider channelTypeProvider;
    private ChannelGroupTypeProvider channelGroupTypeProvider;

    @Before
    public void setUp() {
        // get ONLY the XMLChannelTypeProvider
        channelTypeProvider = getService(ChannelTypeProvider.class,
                serviceReference -> "core.xml.channels".equals(serviceReference.getProperty("esh.scope")));
        assertThat(channelTypeProvider, is(notNullValue()));
        channelGroupTypeProvider = getService(ChannelGroupTypeProvider.class,
                serviceReference -> "core.xml.channelGroups".equals(serviceReference.getProperty("esh.scope")));
        assertThat(channelGroupTypeProvider, is(notNullValue()));
    }

    @After
    public void tearDown() throws Exception {
        SyntheticBundleInstaller.uninstall(bundleContext, TEST_BUNDLE_NAME);
    }

    @Test
    public void ChannelTypesShouldBeLoaded() throws Exception {
        int initialNumberOfChannelTypes = channelTypeProvider.getChannelTypes(null).size();
        int initialNumberOfChannelGroupTypes = channelGroupTypeProvider.getChannelGroupTypes(null).size();

        // install test bundle
        Bundle bundle = SyntheticBundleInstaller.install(bundleContext, TEST_BUNDLE_NAME);
        assertThat(bundle, is(notNullValue()));

        Collection<ChannelType> channelTypes = channelTypeProvider.getChannelTypes(null);
        assertThat(channelTypes.size(), is(initialNumberOfChannelTypes + 2));

        ChannelType channelType1 = channelTypes.stream()
                .filter(it -> it.getUID().toString().equals("somebinding:channel1")).findFirst().get();
        assertThat(channelType1, is(not(nullValue())));

        ChannelType channelType2 = channelTypes.stream()
                .filter(it -> it.getUID().toString().equals("somebinding:channel-without-reference")).findFirst().get();
        assertThat(channelType2, is(not(nullValue())));

        Collection<ChannelGroupType> channelGroupTypes = channelGroupTypeProvider.getChannelGroupTypes(null);
        assertThat(channelGroupTypes.size(), is(initialNumberOfChannelGroupTypes + 1));

        ChannelGroupType channelGroupType = channelGroupTypes.stream()
                .filter(it -> it.getUID().toString().equals("somebinding:channelgroup")).findFirst().get();
        assertThat(channelGroupType, is(not(nullValue())));
        assertThat(channelGroupType.getCategory(), is("Temperature"));

        SyntheticBundleInstaller.uninstall(bundleContext, TEST_BUNDLE_NAME);

        assertThat(channelTypeProvider.getChannelTypes(null).size(), is(initialNumberOfChannelTypes));
        assertThat(channelGroupTypeProvider.getChannelGroupTypes(null).size(), is(initialNumberOfChannelGroupTypes));
    }
}
