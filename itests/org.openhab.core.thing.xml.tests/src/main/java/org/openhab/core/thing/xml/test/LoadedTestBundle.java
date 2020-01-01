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
package org.openhab.core.thing.xml.test;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.core.ConfigDescriptionRegistry;
import org.openhab.core.test.SyntheticBundleInstaller;
import org.openhab.core.test.java.JavaTest;
import org.openhab.core.thing.binding.ThingTypeProvider;
import org.openhab.core.thing.type.ChannelGroupTypeRegistry;
import org.openhab.core.thing.type.ChannelTypeRegistry;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;

/**
 * @author Markus Rathgeb - Initial contribution
 */
public class LoadedTestBundle extends JavaTest implements AutoCloseable {

    @FunctionalInterface
    public interface ServiceProvider {
        <T> @Nullable T getService(Class<T> clazz);
    }

    public static class StuffAddition {
        public int addedThingTypes = -1;
        public int addedChannelTypes = -1;
        public int addedChannelGroupTypes = -1;
        public int addedConfigDescriptions = -1;

        public StuffAddition() {
        }

        StuffAddition thingTypes(int nr) {
            addedThingTypes = nr;
            return this;
        }

        StuffAddition channelTypes(int nr) {
            addedChannelTypes = nr;
            return this;
        }

        StuffAddition channelGroupTypes(int nr) {
            addedChannelGroupTypes = nr;
            return this;
        }

        StuffAddition configDescriptions(int nr) {
            addedConfigDescriptions = nr;
            return this;
        }
    }

    private final ThingTypeProvider thingTypeProvider;
    private final ChannelTypeRegistry channelTypeRegistry;
    private final ChannelGroupTypeRegistry channelGroupTypeRegistry;
    private final ConfigDescriptionRegistry configDescriptionRegistry;

    private final int initialNumberOfThingTypes;
    private final int initialNumberOfChannelTypes;
    private final int initialNumberOfChannelGroupTypes;
    private final int initialNumberOfConfigDescriptions;

    private final Bundle bundle;

    public LoadedTestBundle(String testBundleName, BundleContext bundleContext, ServiceProvider serviceProvider,
            StuffAddition stuffAddition) throws Exception {
        thingTypeProvider = serviceProvider.getService(ThingTypeProvider.class);
        assertThat(thingTypeProvider, is(notNullValue()));

        channelTypeRegistry = serviceProvider.getService(ChannelTypeRegistry.class);
        assertThat(channelTypeRegistry, is(notNullValue()));

        channelGroupTypeRegistry = serviceProvider.getService(ChannelGroupTypeRegistry.class);
        assertThat(channelGroupTypeRegistry, is(notNullValue()));

        configDescriptionRegistry = serviceProvider.getService(ConfigDescriptionRegistry.class);
        assertThat(configDescriptionRegistry, is(notNullValue()));

        initialNumberOfThingTypes = thingTypeProvider.getThingTypes(null).size();
        initialNumberOfChannelTypes = channelTypeRegistry.getChannelTypes().size();
        initialNumberOfChannelGroupTypes = channelGroupTypeRegistry.getChannelGroupTypes().size();
        initialNumberOfConfigDescriptions = configDescriptionRegistry.getConfigDescriptions().size();

        this.bundle = SyntheticBundleInstaller.install(bundleContext, testBundleName);
        assertThat(bundle, is(notNullValue()));

        if (stuffAddition.addedThingTypes >= 0) {
            waitForAssert(() -> {
                assertThat(thingTypeProvider.getThingTypes(null).size(),
                        is(initialNumberOfThingTypes + stuffAddition.addedThingTypes));
            });
        }
        if (stuffAddition.addedChannelTypes >= 0) {
            waitForAssert(() -> {
                assertThat(channelTypeRegistry.getChannelTypes().size(),
                        is(initialNumberOfChannelTypes + stuffAddition.addedChannelTypes));
            });
        }
        if (stuffAddition.addedChannelGroupTypes >= 0) {
            waitForAssert(() -> {
                assertThat(channelGroupTypeRegistry.getChannelGroupTypes().size(),
                        is(initialNumberOfChannelGroupTypes + stuffAddition.addedChannelGroupTypes));
            });
        }
        if (stuffAddition.addedConfigDescriptions >= 0) {
            waitForAssert(() -> {
                assertThat(configDescriptionRegistry.getConfigDescriptions().size(),
                        is(initialNumberOfConfigDescriptions + stuffAddition.addedConfigDescriptions));
            });
        }
    }

    @Override
    public void close() throws BundleException {
        bundle.uninstall();
        assertThat(bundle.getState(), is(Bundle.UNINSTALLED));

        waitForAssert(() -> {
            assertThat(thingTypeProvider.getThingTypes(null).size(), is(initialNumberOfThingTypes));
        });
        waitForAssert(() -> {
            assertThat(channelTypeRegistry.getChannelTypes().size(), is(initialNumberOfChannelTypes));
        });
        waitForAssert(() -> {
            assertThat(channelGroupTypeRegistry.getChannelGroupTypes().size(), is(initialNumberOfChannelGroupTypes));
        });
        waitForAssert(() -> {
            assertThat(configDescriptionRegistry.getConfigDescriptions().size(), is(initialNumberOfConfigDescriptions));
        });
    }
}
