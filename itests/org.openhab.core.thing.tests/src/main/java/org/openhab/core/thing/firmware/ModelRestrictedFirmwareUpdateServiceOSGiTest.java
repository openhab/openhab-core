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
package org.openhab.core.thing.firmware;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.openhab.core.thing.Thing.*;
import static org.openhab.core.thing.firmware.FirmwareStatus.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.openhab.core.test.java.JavaOSGiTest;
import org.openhab.core.thing.ManagedThingProvider;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingRegistry;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.builder.ThingBuilder;
import org.openhab.core.thing.binding.firmware.Firmware;
import org.openhab.core.thing.binding.firmware.FirmwareBuilder;
import org.openhab.core.thing.binding.firmware.FirmwareUpdateHandler;
import org.openhab.core.thing.binding.firmware.ProgressCallback;
import org.openhab.core.thing.type.ThingType;
import org.openhab.core.thing.type.ThingTypeBuilder;

/**
 * OSGi integration tests for the {@link FirmwareUpdateService}, focusing on firmware updates with firmwares that are
 * restricted to a specific model.
 *
 * @author Henning Sudbrock - Initial contribution
 * @author Dimitar Ivanov - replaced Firmware UID with thing UID and firmware version
 */
public class ModelRestrictedFirmwareUpdateServiceOSGiTest extends JavaOSGiTest {

    private static final String THING_ID_1 = "thing1";
    private static final String THING_ID_2 = "thing2";

    private static final String MODEL_ID_1 = "model1";
    private static final String MODEL_ID_2 = "model2";

    private static final String VERSION_1_0_0 = "1.0.0";
    private static final String VERSION_1_0_1 = "1.0.1";
    private static final String VERSION_1_0_2 = "1.0.2";
    private static final String VERSION_1_0_3 = "1.0.3";

    /** The thing type for all things used in this test class */
    private static final ThingType THING_TYPE = ThingTypeBuilder
            .instance(new ThingTypeUID("bindingId", "thingTypeId"), "label").build();

    private ThingRegistry thingRegistry;
    private FirmwareUpdateService firmwareUpdateService;
    private ManagedThingProvider managedThingProvider;

    @Before
    public void setup() {
        registerVolatileStorageService();

        thingRegistry = getService(ThingRegistry.class);
        assertThat(thingRegistry, is(notNullValue()));

        managedThingProvider = getService(ManagedThingProvider.class);
        assertThat(managedThingProvider, is(notNullValue()));

        firmwareUpdateService = getService(FirmwareUpdateService.class);
        assertThat(firmwareUpdateService, is(notNullValue()));
    }

    @Test
    public void testGetModelRestrictedFirmwareStatusInfo() {
        // given two things of the same thing type but with different models, both with firmware 1.0.0 installed
        ThingUID thingUID1 = createAndRegisterThing(THING_ID_1, MODEL_ID_1, VERSION_1_0_0).getUID();
        ThingUID thingUID2 = createAndRegisterThing(THING_ID_2, MODEL_ID_2, VERSION_1_0_0).getUID();

        // given a firmware provider that provides different firmwares for the different models
        registerService(createFirmwareProvider(createModelRestrictedFirmware(MODEL_ID_1, VERSION_1_0_1),
                createModelRestrictedFirmware(MODEL_ID_2, VERSION_1_0_2)));

        // then there is an update to 1.0.1 available for thing1
        FirmwareStatusInfo firmwareStatusInfoA = firmwareUpdateService.getFirmwareStatusInfo(thingUID1);
        assertThat(firmwareStatusInfoA.getFirmwareStatus(), is(UPDATE_EXECUTABLE));
        assertThat(firmwareStatusInfoA.getUpdatableFirmwareVersion(), is(VERSION_1_0_1));

        // and then there is an update to 1.0.2 available for thing2
        FirmwareStatusInfo firmwareStatusInfoB = firmwareUpdateService.getFirmwareStatusInfo(thingUID2);
        assertThat(firmwareStatusInfoB.getFirmwareStatus(), is(UPDATE_EXECUTABLE));
        assertThat(firmwareStatusInfoB.getUpdatableFirmwareVersion(), is(VERSION_1_0_2));
    }

    @Test
    public void testUpdateModelRestrictedFirmware() {
        // given two things of the same thing type but with different models, both with firmware 1.0.0 installed
        ThingUID thingUID1 = createAndRegisterThing(THING_ID_1, MODEL_ID_1, VERSION_1_0_0).getUID();
        ThingUID thingUID2 = createAndRegisterThing(THING_ID_2, MODEL_ID_2, VERSION_1_0_0).getUID();

        // given a firmware provider that provides different firmwares for the different models
        Firmware firmwareModelA = createModelRestrictedFirmware(MODEL_ID_1, VERSION_1_0_1);
        Firmware firmwareModelB = createModelRestrictedFirmware(MODEL_ID_2, VERSION_1_0_2);
        registerService(createFirmwareProvider(firmwareModelA, firmwareModelB));

        // when the firmware on thing1 is updated to the firmware for model1
        firmwareUpdateService.updateFirmware(thingUID1, firmwareModelA.getVersion(), null);

        // then the new firmware is installed on thing 1 and no more update is available
        waitForAssert(() -> {
            assertThatThingHasFirmware(thingUID1, VERSION_1_0_1);
            assertThatThingHasFirmwareStatus(thingUID1, UP_TO_DATE);
        });

        // and when the firmware on thing2 is updated to the firmware for model2
        firmwareUpdateService.updateFirmware(thingUID2, firmwareModelB.getVersion(), null);

        // then the new firmware is installed on thing2 and no more update is available
        waitForAssert(() -> {
            assertThatThingHasFirmware(thingUID2, VERSION_1_0_2);
            assertThatThingHasFirmwareStatus(thingUID2, UP_TO_DATE);
        });
    }

    @Test
    public void testUpdateModelRestrictedFirmwareMultipleFirmwareProviders() {
        // given two things of the same thing type but with different models and with different firmwares installed
        ThingUID thingUID1 = createAndRegisterThing(THING_ID_1, MODEL_ID_1, VERSION_1_0_1).getUID();
        ThingUID thingUID2 = createAndRegisterThing(THING_ID_2, MODEL_ID_2, VERSION_1_0_2).getUID();

        // given a firmware provider that provides the current firmware for both things,
        // and another firmware provider that provides a more recent firmware for the first model
        Firmware firmwareModelA = createModelRestrictedFirmware(MODEL_ID_1, VERSION_1_0_1);
        Firmware firmwareModelB = createModelRestrictedFirmware(MODEL_ID_2, VERSION_1_0_2);
        Firmware firmwareModelC = createModelRestrictedFirmware(MODEL_ID_1, VERSION_1_0_3);
        registerService(createFirmwareProvider(firmwareModelA, firmwareModelB));
        registerService(createFirmwareProvider(firmwareModelC));

        // then there is an update to 1.0.3 available for thing1
        FirmwareStatusInfo firmwareStatusInfoA = firmwareUpdateService.getFirmwareStatusInfo(thingUID1);
        assertThat(firmwareStatusInfoA.getFirmwareStatus(), is(UPDATE_EXECUTABLE));
        assertThat(firmwareStatusInfoA.getUpdatableFirmwareVersion(), is(VERSION_1_0_3));

        // but there is no update available for thing2
        assertThatThingHasFirmwareStatus(thingUID2, UP_TO_DATE);

        // and when the firmware on thing1 is updated
        firmwareUpdateService.updateFirmware(thingUID1, firmwareModelC.getVersion(), null);

        // then the new firmware is installed on thing 1 and no more update is available
        waitForAssert(() -> {
            assertThatThingHasFirmware(thingUID1, VERSION_1_0_3);
            assertThatThingHasFirmwareStatus(thingUID1, UP_TO_DATE);
        });
    }

    /**
     * Creates a thing, adds it to the thing registry, and registers a {@link FirmwareUpdateHandler} for the thing.
     */
    private Thing createAndRegisterThing(String thingUID, String modelId, String firmwareVersion) {
        Thing thing = ThingBuilder.create(THING_TYPE.getUID(), thingUID).build();
        thing.setProperty(PROPERTY_MODEL_ID, modelId);
        thing.setProperty(PROPERTY_FIRMWARE_VERSION, firmwareVersion);

        managedThingProvider.add(thing);

        registerService(createFirmwareUpdateHandler(thing));

        return thing;
    }

    /**
     * Create a firmware update handler for the given thing that sets the thing's firmware property upon update.
     */
    private FirmwareUpdateHandler createFirmwareUpdateHandler(final Thing thing) {
        return new FirmwareUpdateHandler() {

            @Override
            public void updateFirmware(Firmware firmware, ProgressCallback progressCallback) {
                getThing().setProperty(Thing.PROPERTY_FIRMWARE_VERSION, firmware.getVersion());
            }

            @Override
            public boolean isUpdateExecutable() {
                return true;
            }

            @Override
            public Thing getThing() {
                Thing ret = thingRegistry.get(thing.getUID());
                if (ret == null) {
                    ret = thing;
                }
                return ret;
            }

            @Override
            public void cancel() {
                // do nothing
            }
        };
    }

    /**
     * Creates a {@link FirmwareProvider} that provides firmwares for the global thing type used in this
     * test.
     */
    private FirmwareProvider createFirmwareProvider(Firmware... firmwares) {
        return new FirmwareProvider() {

            @Override
            public Set<Firmware> getFirmwares(Thing thing, Locale locale) {
                if (thing.getThingTypeUID().equals(THING_TYPE.getUID())) {
                    return new HashSet<>(Arrays.asList(firmwares));
                } else {
                    return Collections.emptySet();
                }
            }

            @Override
            public Set<Firmware> getFirmwares(Thing thing) {
                return getFirmwares(thing, null);
            }

            @Override
            public Firmware getFirmware(Thing thing, String version, Locale locale) {
                for (Firmware firmware : getFirmwares(thing, locale)) {
                    if (firmware.getVersion().equals(version)) {
                        return firmware;
                    }
                }
                return null;
            }

            @Override
            public Firmware getFirmware(Thing thing, String version) {
                return getFirmware(thing, version, null);
            }
        };
    }

    private Firmware createModelRestrictedFirmware(String model, String version) {
        return FirmwareBuilder.create(THING_TYPE.getUID(), version).withModel(model).withModelRestricted(true).build();
    }

    private void assertThatThingHasFirmware(ThingUID thingUID, String firmwareVersion) {
        Thing thing = thingRegistry.get(thingUID);
        assertThat(thing, is(notNullValue()));
        assertThat(thing.getProperties().get(PROPERTY_FIRMWARE_VERSION), is(firmwareVersion));
    }

    private void assertThatThingHasFirmwareStatus(ThingUID thingUID, FirmwareStatus firmwareStatus) {
        assertThat(firmwareUpdateService.getFirmwareStatusInfo(thingUID).getFirmwareStatus(), is(firmwareStatus));
    }

}
