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
package org.eclipse.smarthome.core.thing.internal.firmware;

import static org.eclipse.smarthome.core.thing.firmware.Constants.*;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.i18n.LocaleProvider;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.binding.builder.ThingBuilder;
import org.eclipse.smarthome.core.thing.binding.firmware.Firmware;
import org.eclipse.smarthome.core.thing.firmware.Constants;
import org.eclipse.smarthome.core.thing.firmware.FirmwareProvider;
import org.eclipse.smarthome.core.thing.firmware.FirmwareRegistry;
import org.eclipse.smarthome.core.thing.testutil.i18n.DefaultLocaleSetter;
import org.eclipse.smarthome.test.java.JavaOSGiTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * Testing the {@link FirmwareRegistry}.
 *
 * @author Thomas Höfer - Initial contribution
 * @author Dimitar Ivanov - Adapted the test for registry using thing and version instead of firmware UID
 * @author Wouter Born - Migrate tests from Groovy to Java
 */
public class FirmwareRegistryOSGiTest extends JavaOSGiTest {

    private static final int FW1 = 0;
    private static final int FW2 = 1;
    private static final int FW3 = 2;

    private Locale defaultLocale;

    private FirmwareRegistry firmwareRegistry;

    @NonNullByDefault
    private FirmwareProvider basicFirmwareProviderMock = new FirmwareProvider() {
        @Override
        public @Nullable Firmware getFirmware(Thing thing, String version, @Nullable Locale locale) {
            if (!thing.equals(thing1)) {
                return null;
            }

            if (Locale.ENGLISH.equals(locale)) {
                if (version.equals(FW111_EN.getVersion())) {
                    return FW111_EN;
                } else if (version.equals(FW112_EN.getVersion())) {
                    return FW112_EN;
                }
            } else {
                if (version.equals(FW111_DE.getVersion())) {
                    return FW111_DE;
                } else if (version.equals(FW112_DE.getVersion())) {
                    return FW112_DE;
                }
            }
            return null;
        }

        @Override
        public @Nullable Set<Firmware> getFirmwares(Thing thing, @Nullable Locale locale) {
            if (!thing.equals(thing1)) {
                return Collections.emptySet();
            }
            if (Locale.ENGLISH.equals(locale)) {
                return Stream.of(FW111_EN, FW112_EN).collect(Collectors.toSet());
            } else {
                return Stream.of(FW111_DE, FW112_DE).collect(Collectors.toSet());
            }
        }

        @Override
        public @Nullable Firmware getFirmware(Thing thing, String version) {
            return getFirmware(thing, version, null);
        }

        @Override
        public @Nullable Set<Firmware> getFirmwares(Thing thing) {
            return getFirmwares(thing, null);
        }

    };

    @NonNullByDefault
    private FirmwareProvider additionalFirmwareProviderMock = new FirmwareProvider() {
        @Override
        public @Nullable Firmware getFirmware(Thing thing, String version, @Nullable Locale locale) {
            if (version.equals(FW111_FIX_EN.getVersion())) {
                if (Locale.ENGLISH.equals(locale)) {
                    return FW111_FIX_EN;
                } else {
                    return FW111_FIX_DE;
                }
            } else if (version.equals(FWALPHA_EN.getVersion())) {
                if (Locale.ENGLISH.equals(locale)) {
                    return FWALPHA_EN;
                } else {
                    return FWALPHA_DE;
                }
            } else if (version.equals(FWBETA_EN.getVersion())) {
                if (Locale.ENGLISH.equals(locale)) {
                    return FWBETA_EN;
                } else {
                    return FWBETA_DE;
                }
            } else if (version.equals(FWGAMMA_EN.getVersion())) {
                if (Locale.ENGLISH.equals(locale)) {
                    return FWGAMMA_EN;
                } else {
                    return FWGAMMA_DE;
                }
            }
            return null;
        }

        @Override
        public @Nullable Set<Firmware> getFirmwares(Thing thing, @Nullable Locale locale) {
            if (thing.getThingTypeUID().equals(THING_TYPE_UID1)) {
                if (Locale.ENGLISH.equals(locale)) {
                    return Collections.singleton(FW111_FIX_EN);
                } else {
                    return Collections.singleton(FW111_FIX_DE);
                }
            } else if (thing.getThingTypeUID().equals(THING_TYPE_UID2)) {
                if (Locale.ENGLISH.equals(locale)) {
                    return Stream.of(FWALPHA_EN, FWBETA_EN, FWGAMMA_EN).collect(Collectors.toSet());
                } else {
                    return Stream.of(FWALPHA_DE, FWBETA_DE, FWGAMMA_DE).collect(Collectors.toSet());
                }
            }
            return null;
        }

        @Override
        public @Nullable Firmware getFirmware(Thing thing, String version) {
            return getFirmware(thing, version, null);
        }

        @Override
        public @Nullable Set<Firmware> getFirmwares(Thing thing) {
            return getFirmwares(thing, null);
        }
    };

    @NonNullByDefault
    private FirmwareProvider nullProviderMock = new FirmwareProvider() {
        @Override
        public @Nullable Firmware getFirmware(Thing thing, String version, @Nullable Locale locale) {
            return null;
        }

        @Override
        public @Nullable Set<Firmware> getFirmwares(Thing thing, @Nullable Locale locale) {
            return null;
        }

        @Override
        public @Nullable Firmware getFirmware(Thing thing, String version) {
            return null;
        }

        @Override
        public @Nullable Set<Firmware> getFirmwares(Thing thing) {
            return null;
        }
    };
    private Thing thing1;
    private Thing thing2;
    private Thing thing3;

    @Before
    public void setup() throws IOException {
        LocaleProvider localeProvider = getService(LocaleProvider.class);
        assertThat(localeProvider, is(notNullValue()));
        defaultLocale = localeProvider.getLocale();

        new DefaultLocaleSetter(getService(ConfigurationAdmin.class)).setDefaultLocale(Locale.ENGLISH);
        waitForAssert(() -> assertThat(localeProvider.getLocale(), is(Locale.ENGLISH)));

        firmwareRegistry = getService(FirmwareRegistry.class);
        assertThat(firmwareRegistry, is(notNullValue()));

        registerService(basicFirmwareProviderMock);

        thing1 = ThingBuilder.create(THING_TYPE_UID1, THING1_ID).build();
        thing2 = ThingBuilder.create(THING_TYPE_UID1, THING2_ID).build();
        thing3 = ThingBuilder.create(THING_TYPE_UID2, THING3_ID).build();
    }

    @After
    public void teardown() throws IOException {
        new DefaultLocaleSetter(getService(ConfigurationAdmin.class)).setDefaultLocale(defaultLocale);
        waitForAssert(() -> assertThat(getService(LocaleProvider.class).getLocale(), is(defaultLocale)));
    }

    @Test
    public void assertThatRegistryWorksWithSingleProvider() {
        List<Firmware> firmwares = new ArrayList<>(firmwareRegistry.getFirmwares(thing1));
        assertThat(firmwares.size(), is(2));
        assertThat(firmwares.get(FW1), is(FW112_EN));
        assertThat(firmwares.get(FW2), is(FW111_EN));

        firmwares = new ArrayList<>(firmwareRegistry.getFirmwares(thing1, Locale.ENGLISH));
        assertThat(firmwares.size(), is(2));
        assertThat(firmwares.get(FW1), is(FW112_EN));
        assertThat(firmwares.get(FW2), is(FW111_EN));

        firmwares = new ArrayList<>(firmwareRegistry.getFirmwares(thing1, Locale.GERMAN));
        assertThat(firmwares.size(), is(2));
        assertThat(firmwares.get(FW1), is(FW112_DE));
        assertThat(firmwares.get(FW2), is(FW111_DE));

        Firmware firmware = firmwareRegistry.getFirmware(thing1, V111);
        assertThat(firmware, is(FW111_EN));

        firmware = firmwareRegistry.getFirmware(thing1, V112, Locale.ENGLISH);
        assertThat(firmware, is(FW112_EN));

        firmware = firmwareRegistry.getFirmware(thing1, V112, Locale.GERMAN);
        assertThat(firmware, is(FW112_DE));
    }

    @Test
    public void assertThatRegistryWorksWithSeveralProviders() {
        List<Firmware> firmwares = new ArrayList<>(firmwareRegistry.getFirmwares(thing1));
        assertThat(firmwares.size(), is(2));

        firmwares = new ArrayList<>(firmwareRegistry.getFirmwares(thing2));
        assertThat(firmwares.size(), is(0));

        registerService(additionalFirmwareProviderMock);

        firmwares = new ArrayList<>(firmwareRegistry.getFirmwares(thing1));
        assertThat(firmwares.size(), is(3));
        assertThat(firmwares.get(FW1), is(FW112_EN));
        assertThat(firmwares.get(FW2), is(FW111_FIX_EN));
        assertThat(firmwares.get(FW3), is(FW111_EN));

        firmwares = new ArrayList<>(firmwareRegistry.getFirmwares(thing1, Locale.ENGLISH));
        assertThat(firmwares.size(), is(3));
        assertThat(firmwares.get(FW1), is(FW112_EN));
        assertThat(firmwares.get(FW2), is(FW111_FIX_EN));
        assertThat(firmwares.get(FW3), is(FW111_EN));

        firmwares = new ArrayList<>(firmwareRegistry.getFirmwares(thing1, Locale.GERMAN));
        assertThat(firmwares.size(), is(3));
        assertThat(firmwares.get(FW1), is(FW112_DE));
        assertThat(firmwares.get(FW2), is(FW111_FIX_DE));
        assertThat(firmwares.get(FW3), is(FW111_DE));

        Firmware firmware = firmwareRegistry.getFirmware(thing1, V111_FIX);
        assertThat(firmware, is(FW111_FIX_EN));

        firmware = firmwareRegistry.getFirmware(thing1, V111_FIX, Locale.ENGLISH);
        assertThat(firmware, is(FW111_FIX_EN));

        firmware = firmwareRegistry.getFirmware(thing1, V111_FIX, Locale.GERMAN);
        assertThat(firmware, is(FW111_FIX_DE));

        firmwares = new ArrayList<>(firmwareRegistry.getFirmwares(thing3));
        assertThat(firmwares.size(), is(3));
        assertThat(firmwares.get(FW1), is(FWGAMMA_EN));
        assertThat(firmwares.get(FW2), is(FWBETA_EN));
        assertThat(firmwares.get(FW3), is(FWALPHA_EN));

        firmwares = new ArrayList<>(firmwareRegistry.getFirmwares(thing3, Locale.ENGLISH));
        assertThat(firmwares.size(), is(3));
        assertThat(firmwares.get(FW1), is(FWGAMMA_EN));
        assertThat(firmwares.get(FW2), is(FWBETA_EN));
        assertThat(firmwares.get(FW3), is(FWALPHA_EN));

        firmwares = new ArrayList<>(firmwareRegistry.getFirmwares(thing3, Locale.GERMAN));
        assertThat(firmwares.size(), is(3));
        assertThat(firmwares.get(FW1), is(FWGAMMA_DE));
        assertThat(firmwares.get(FW2), is(FWBETA_DE));
        assertThat(firmwares.get(FW3), is(FWALPHA_DE));

        firmware = firmwareRegistry.getFirmware(thing3, VALPHA);
        assertThat(firmware, is(FWALPHA_EN));

        firmware = firmwareRegistry.getFirmware(thing3, VALPHA, Locale.ENGLISH);
        assertThat(firmware, is(FWALPHA_EN));

        firmware = firmwareRegistry.getFirmware(thing3, VALPHA, Locale.GERMAN);
        assertThat(firmware, is(FWALPHA_DE));

        firmware = firmwareRegistry.getFirmware(thing3, VBETA);
        assertThat(firmware, is(FWBETA_EN));

        firmware = firmwareRegistry.getFirmware(thing3, VBETA, Locale.ENGLISH);
        assertThat(firmware, is(FWBETA_EN));

        firmware = firmwareRegistry.getFirmware(thing3, VBETA, Locale.GERMAN);
        assertThat(firmware, is(FWBETA_DE));

        firmware = firmwareRegistry.getFirmware(thing3, VGAMMA);
        assertThat(firmware, is(FWGAMMA_EN));

        firmware = firmwareRegistry.getFirmware(thing3, VGAMMA, Locale.ENGLISH);
        assertThat(firmware, is(FWGAMMA_EN));

        firmware = firmwareRegistry.getFirmware(thing3, VGAMMA, Locale.GERMAN);
        assertThat(firmware, is(FWGAMMA_DE));
    }

    @Test
    public void assertThatRegistryReturnsEmptySetForThingAndGetFirmwaresOperation() {
        Collection<Firmware> firmwares = firmwareRegistry.getFirmwares(thing3);
        assertThat(firmwares.size(), is(0));

        firmwares = firmwareRegistry.getFirmwares(thing3, null);
        assertThat(firmwares.size(), is(0));
    }

    @Test
    public void assertThatRegistryReturnsNullForUnknownFirmwareUidAndGetFirmwareOperation() {
        Firmware firmware = firmwareRegistry.getFirmware(thing1, Constants.UNKNOWN);
        assertThat(firmware, is(nullValue()));

        firmware = firmwareRegistry.getFirmware(thing2, VALPHA, Locale.GERMAN);
        assertThat(firmware, is(nullValue()));
    }

    public void assertThatRegistryReturnsCorrectLatestFirmware() {
        registerService(additionalFirmwareProviderMock);

        Firmware firmware = firmwareRegistry.getLatestFirmware(thing1);
        assertThat(firmware, is(FW112_EN));

        firmware = firmwareRegistry.getLatestFirmware(thing1, Locale.ENGLISH);
        assertThat(firmware, is(FW112_EN));

        firmware = firmwareRegistry.getLatestFirmware(thing1, Locale.GERMAN);
        assertThat(firmware, is(FW112_DE));

        firmware = firmwareRegistry.getLatestFirmware(thing2);
        assertThat(firmware, is(FWALPHA_EN));

        firmware = firmwareRegistry.getLatestFirmware(thing2, Locale.ENGLISH);
        assertThat(firmware, is(FWALPHA_EN));

        firmware = firmwareRegistry.getLatestFirmware(thing2, Locale.GERMAN);
        assertThat(firmware, is(FWALPHA_DE));
    }

    @Test
    public void assertThatFirmwarePropertiesAreProvided() {
        registerService(additionalFirmwareProviderMock);

        Firmware firmware = firmwareRegistry.getFirmware(thing3, FWALPHA_DE.getVersion());
        assertThat(firmware, is(notNullValue()));
        assertThat(firmware.getProperties(), is(notNullValue()));
        assertThat(firmware.getProperties().isEmpty(), is(true));

        firmware = firmwareRegistry.getFirmware(thing3, FWBETA_DE.getVersion());
        assertThat(firmware, is(notNullValue()));
        assertThat(firmware.getProperties(), is(notNullValue()));
        assertThat(firmware.getProperties().size(), is(1));
        assertThat(firmware.getProperties().get(Firmware.PROPERTY_REQUIRES_FACTORY_RESET), is("true"));

        firmware = firmwareRegistry.getFirmware(thing3, FWGAMMA_DE.getVersion());
        assertThat(firmware, is(notNullValue()));
        assertThat(firmware.getProperties(), is(notNullValue()));
        assertThat(firmware.getProperties().size(), is(2));
        assertThat(firmware.getProperties().get("prop1"), is("a"));
        assertThat(firmware.getProperties().get("prop2"), is("b"));
    }

    @Test
    public void assertThatRegistryReturnsNullForUnknownThingTypeUidAndGetLatestFirmwareOperation() {
        Firmware firmware = firmwareRegistry.getLatestFirmware(thing3);
        assertThat(firmware, is(nullValue()));

        firmware = firmwareRegistry.getLatestFirmware(thing3, Locale.GERMAN);
        assertThat(firmware, is(nullValue()));
    }

    @Test
    public void assertFirmwareProviderReturningNullResults() {
        unregisterService(basicFirmwareProviderMock);
        registerService(nullProviderMock);

        firmwareRegistry.getFirmwares(thing3);
        firmwareRegistry.getFirmware(thing3, FWALPHA_DE.getVersion());
    }

    @Test
    public void assertFirmwareThatInvalidProvidersAreSkipped() {
        registerService(nullProviderMock);

        List<Firmware> firmwares = new ArrayList<>(firmwareRegistry.getFirmwares(thing1));
        assertThat(firmwares.size(), is(2));
        assertThat(firmwares.get(FW1), is(FW112_EN));
        assertThat(firmwares.get(FW2), is(FW111_EN));
    }
}