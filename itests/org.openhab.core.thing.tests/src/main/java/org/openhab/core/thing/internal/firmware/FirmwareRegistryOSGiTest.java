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
package org.openhab.core.thing.internal.firmware;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.openhab.core.thing.firmware.Constants.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openhab.core.i18n.LocaleProvider;
import org.openhab.core.test.java.JavaOSGiTest;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.binding.builder.ThingBuilder;
import org.openhab.core.thing.binding.firmware.Firmware;
import org.openhab.core.thing.firmware.Constants;
import org.openhab.core.thing.firmware.FirmwareProvider;
import org.openhab.core.thing.firmware.FirmwareRegistry;
import org.openhab.core.thing.testutil.i18n.DefaultLocaleSetter;
import org.osgi.service.cm.ConfigurationAdmin;

/**
 * Testing the {@link FirmwareRegistry}.
 *
 * @author Thomas Höfer - Initial contribution
 * @author Dimitar Ivanov - Adapted the test for registry using thing and version instead of firmware UID
 * @author Wouter Born - Migrate tests from Groovy to Java
 */
@NonNullByDefault
public class FirmwareRegistryOSGiTest extends JavaOSGiTest {

    private static final int FW1 = 0;
    private static final int FW2 = 1;
    private static final int FW3 = 2;

    private static final Thing THING1 = ThingBuilder.create(THING_TYPE_UID1, THING1_ID).build();
    private static final Thing THING2 = ThingBuilder.create(THING_TYPE_UID1, THING2_ID).build();
    private static final Thing THING3 = ThingBuilder.create(THING_TYPE_UID2, THING3_ID).build();

    private @NonNullByDefault({}) ConfigurationAdmin configurationAdmin;
    private @NonNullByDefault({}) Locale defaultLocale;
    private @NonNullByDefault({}) FirmwareRegistry firmwareRegistry;

    private FirmwareProvider basicFirmwareProviderMock = new FirmwareProvider() {
        @Override
        public @Nullable Firmware getFirmware(Thing thing, String version, @Nullable Locale locale) {
            if (!thing.equals(THING1)) {
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
            if (!thing.equals(THING1)) {
                return Set.of();
            }
            if (Locale.ENGLISH.equals(locale)) {
                return Set.of(FW111_EN, FW112_EN);
            } else {
                return Set.of(FW111_DE, FW112_DE);
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
            if (THING_TYPE_UID1.equals(thing.getThingTypeUID())) {
                if (Locale.ENGLISH.equals(locale)) {
                    return Set.of(FW111_FIX_EN);
                } else {
                    return Set.of(FW111_FIX_DE);
                }
            } else if (THING_TYPE_UID2.equals(thing.getThingTypeUID())) {
                if (Locale.ENGLISH.equals(locale)) {
                    return Set.of(FWALPHA_EN, FWBETA_EN, FWGAMMA_EN);
                } else {
                    return Set.of(FWALPHA_DE, FWBETA_DE, FWGAMMA_DE);
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

    @BeforeEach
    public void setup() throws IOException {
        configurationAdmin = getService(ConfigurationAdmin.class);
        assertNotNull(configurationAdmin);

        LocaleProvider localeProvider = getService(LocaleProvider.class);
        assertThat(localeProvider, is(notNullValue()));
        defaultLocale = localeProvider.getLocale();

        new DefaultLocaleSetter(configurationAdmin).setDefaultLocale(Locale.ENGLISH);
        waitForAssert(() -> assertThat(localeProvider.getLocale(), is(Locale.ENGLISH)));

        firmwareRegistry = getService(FirmwareRegistry.class);
        assertThat(firmwareRegistry, is(notNullValue()));

        registerService(basicFirmwareProviderMock);
    }

    @AfterEach
    public void teardown() throws IOException {
        new DefaultLocaleSetter(configurationAdmin).setDefaultLocale(defaultLocale);
        waitForAssert(() -> assertThat(getService(LocaleProvider.class).getLocale(), is(defaultLocale)));
    }

    @Test
    public void assertThatRegistryWorksWithSingleProvider() {
        List<Firmware> firmwares = new ArrayList<>(firmwareRegistry.getFirmwares(THING1));
        assertThat(firmwares.size(), is(2));
        assertThat(firmwares.get(FW1), is(FW112_EN));
        assertThat(firmwares.get(FW2), is(FW111_EN));

        firmwares = new ArrayList<>(firmwareRegistry.getFirmwares(THING1, Locale.ENGLISH));
        assertThat(firmwares.size(), is(2));
        assertThat(firmwares.get(FW1), is(FW112_EN));
        assertThat(firmwares.get(FW2), is(FW111_EN));

        firmwares = new ArrayList<>(firmwareRegistry.getFirmwares(THING1, Locale.GERMAN));
        assertThat(firmwares.size(), is(2));
        assertThat(firmwares.get(FW1), is(FW112_DE));
        assertThat(firmwares.get(FW2), is(FW111_DE));

        Firmware firmware = firmwareRegistry.getFirmware(THING1, V111);
        assertThat(firmware, is(FW111_EN));

        firmware = firmwareRegistry.getFirmware(THING1, V112, Locale.ENGLISH);
        assertThat(firmware, is(FW112_EN));

        firmware = firmwareRegistry.getFirmware(THING1, V112, Locale.GERMAN);
        assertThat(firmware, is(FW112_DE));
    }

    @Test
    public void assertThatRegistryWorksWithSeveralProviders() {
        List<Firmware> firmwares = new ArrayList<>(firmwareRegistry.getFirmwares(THING1));
        assertThat(firmwares.size(), is(2));

        firmwares = new ArrayList<>(firmwareRegistry.getFirmwares(THING2));
        assertThat(firmwares.size(), is(0));

        registerService(additionalFirmwareProviderMock);

        firmwares = new ArrayList<>(firmwareRegistry.getFirmwares(THING1));
        assertThat(firmwares.size(), is(3));
        assertThat(firmwares.get(FW1), is(FW112_EN));
        assertThat(firmwares.get(FW2), is(FW111_FIX_EN));
        assertThat(firmwares.get(FW3), is(FW111_EN));

        firmwares = new ArrayList<>(firmwareRegistry.getFirmwares(THING1, Locale.ENGLISH));
        assertThat(firmwares.size(), is(3));
        assertThat(firmwares.get(FW1), is(FW112_EN));
        assertThat(firmwares.get(FW2), is(FW111_FIX_EN));
        assertThat(firmwares.get(FW3), is(FW111_EN));

        firmwares = new ArrayList<>(firmwareRegistry.getFirmwares(THING1, Locale.GERMAN));
        assertThat(firmwares.size(), is(3));
        assertThat(firmwares.get(FW1), is(FW112_DE));
        assertThat(firmwares.get(FW2), is(FW111_FIX_DE));
        assertThat(firmwares.get(FW3), is(FW111_DE));

        Firmware firmware = firmwareRegistry.getFirmware(THING1, V111_FIX);
        assertThat(firmware, is(FW111_FIX_EN));

        firmware = firmwareRegistry.getFirmware(THING1, V111_FIX, Locale.ENGLISH);
        assertThat(firmware, is(FW111_FIX_EN));

        firmware = firmwareRegistry.getFirmware(THING1, V111_FIX, Locale.GERMAN);
        assertThat(firmware, is(FW111_FIX_DE));

        firmwares = new ArrayList<>(firmwareRegistry.getFirmwares(THING3));
        assertThat(firmwares.size(), is(3));
        assertThat(firmwares.get(FW1), is(FWGAMMA_EN));
        assertThat(firmwares.get(FW2), is(FWBETA_EN));
        assertThat(firmwares.get(FW3), is(FWALPHA_EN));

        firmwares = new ArrayList<>(firmwareRegistry.getFirmwares(THING3, Locale.ENGLISH));
        assertThat(firmwares.size(), is(3));
        assertThat(firmwares.get(FW1), is(FWGAMMA_EN));
        assertThat(firmwares.get(FW2), is(FWBETA_EN));
        assertThat(firmwares.get(FW3), is(FWALPHA_EN));

        firmwares = new ArrayList<>(firmwareRegistry.getFirmwares(THING3, Locale.GERMAN));
        assertThat(firmwares.size(), is(3));
        assertThat(firmwares.get(FW1), is(FWGAMMA_DE));
        assertThat(firmwares.get(FW2), is(FWBETA_DE));
        assertThat(firmwares.get(FW3), is(FWALPHA_DE));

        firmware = firmwareRegistry.getFirmware(THING3, VALPHA);
        assertThat(firmware, is(FWALPHA_EN));

        firmware = firmwareRegistry.getFirmware(THING3, VALPHA, Locale.ENGLISH);
        assertThat(firmware, is(FWALPHA_EN));

        firmware = firmwareRegistry.getFirmware(THING3, VALPHA, Locale.GERMAN);
        assertThat(firmware, is(FWALPHA_DE));

        firmware = firmwareRegistry.getFirmware(THING3, VBETA);
        assertThat(firmware, is(FWBETA_EN));

        firmware = firmwareRegistry.getFirmware(THING3, VBETA, Locale.ENGLISH);
        assertThat(firmware, is(FWBETA_EN));

        firmware = firmwareRegistry.getFirmware(THING3, VBETA, Locale.GERMAN);
        assertThat(firmware, is(FWBETA_DE));

        firmware = firmwareRegistry.getFirmware(THING3, VGAMMA);
        assertThat(firmware, is(FWGAMMA_EN));

        firmware = firmwareRegistry.getFirmware(THING3, VGAMMA, Locale.ENGLISH);
        assertThat(firmware, is(FWGAMMA_EN));

        firmware = firmwareRegistry.getFirmware(THING3, VGAMMA, Locale.GERMAN);
        assertThat(firmware, is(FWGAMMA_DE));
    }

    @Test
    public void assertThatRegistryReturnsEmptySetForThingAndGetFirmwaresOperation() {
        Collection<Firmware> firmwares = firmwareRegistry.getFirmwares(THING3);
        assertThat(firmwares.size(), is(0));

        firmwares = firmwareRegistry.getFirmwares(THING3, null);
        assertThat(firmwares.size(), is(0));
    }

    @Test
    public void assertThatRegistryReturnsNullForUnknownFirmwareUidAndGetFirmwareOperation() {
        Firmware firmware = firmwareRegistry.getFirmware(THING1, Constants.UNKNOWN);
        assertThat(firmware, is(nullValue()));

        firmware = firmwareRegistry.getFirmware(THING2, VALPHA, Locale.GERMAN);
        assertThat(firmware, is(nullValue()));
    }

    public void assertThatRegistryReturnsCorrectLatestFirmware() {
        registerService(additionalFirmwareProviderMock);

        Firmware firmware = firmwareRegistry.getLatestFirmware(THING1);
        assertThat(firmware, is(FW112_EN));

        firmware = firmwareRegistry.getLatestFirmware(THING1, Locale.ENGLISH);
        assertThat(firmware, is(FW112_EN));

        firmware = firmwareRegistry.getLatestFirmware(THING1, Locale.GERMAN);
        assertThat(firmware, is(FW112_DE));

        firmware = firmwareRegistry.getLatestFirmware(THING2);
        assertThat(firmware, is(FWALPHA_EN));

        firmware = firmwareRegistry.getLatestFirmware(THING2, Locale.ENGLISH);
        assertThat(firmware, is(FWALPHA_EN));

        firmware = firmwareRegistry.getLatestFirmware(THING2, Locale.GERMAN);
        assertThat(firmware, is(FWALPHA_DE));
    }

    @Test
    public void assertThatFirmwarePropertiesAreProvided() {
        registerService(additionalFirmwareProviderMock);

        Firmware firmware = firmwareRegistry.getFirmware(THING3, FWALPHA_DE.getVersion());
        assertThat(firmware, is(notNullValue()));
        assertThat(firmware.getProperties(), is(notNullValue()));
        assertThat(firmware.getProperties().isEmpty(), is(true));

        firmware = firmwareRegistry.getFirmware(THING3, FWBETA_DE.getVersion());
        assertThat(firmware, is(notNullValue()));
        assertThat(firmware.getProperties(), is(notNullValue()));
        assertThat(firmware.getProperties().size(), is(1));
        assertThat(firmware.getProperties().get(Firmware.PROPERTY_REQUIRES_FACTORY_RESET), is("true"));

        firmware = firmwareRegistry.getFirmware(THING3, FWGAMMA_DE.getVersion());
        assertThat(firmware, is(notNullValue()));
        assertThat(firmware.getProperties(), is(notNullValue()));
        assertThat(firmware.getProperties().size(), is(2));
        assertThat(firmware.getProperties().get("prop1"), is("a"));
        assertThat(firmware.getProperties().get("prop2"), is("b"));
    }

    @Test
    public void assertThatRegistryReturnsNullForUnknownThingTypeUidAndGetLatestFirmwareOperation() {
        Firmware firmware = firmwareRegistry.getLatestFirmware(THING3);
        assertThat(firmware, is(nullValue()));

        firmware = firmwareRegistry.getLatestFirmware(THING3, Locale.GERMAN);
        assertThat(firmware, is(nullValue()));
    }

    @Test
    public void assertFirmwareProviderReturningNullResults() {
        unregisterService(basicFirmwareProviderMock);
        registerService(nullProviderMock);

        firmwareRegistry.getFirmwares(THING3);
        firmwareRegistry.getFirmware(THING3, FWALPHA_DE.getVersion());
    }

    @Test
    public void assertFirmwareThatInvalidProvidersAreSkipped() {
        registerService(nullProviderMock);

        List<Firmware> firmwares = new ArrayList<>(firmwareRegistry.getFirmwares(THING1));
        assertThat(firmwares.size(), is(2));
        assertThat(firmwares.get(FW1), is(FW112_EN));
        assertThat(firmwares.get(FW2), is(FW111_EN));
    }
}
