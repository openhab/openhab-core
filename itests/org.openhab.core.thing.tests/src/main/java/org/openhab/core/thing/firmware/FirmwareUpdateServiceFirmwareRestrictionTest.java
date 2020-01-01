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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openhab.core.test.java.JavaOSGiTest;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.firmware.Firmware;
import org.openhab.core.thing.binding.firmware.FirmwareBuilder;
import org.openhab.core.thing.binding.firmware.FirmwareRestriction;
import org.openhab.core.thing.binding.firmware.FirmwareUpdateHandler;

/**
 * Tests that the firmware restriction of the {@link Firmware} is respected by the {@link FirmwareUpdateService}
 *
 * @author Dimitar Ivanov - Initial contribution
 */
public class FirmwareUpdateServiceFirmwareRestrictionTest extends JavaOSGiTest {
    private static final String FW_VERSION_32 = "32";
    private static final String FW_VERSION_40 = "40";
    private static final String FW_VERSION_38 = "38";
    private static final String FIRMWARE_VERSION_2 = "2.0.0";
    private static final String FIRMWARE_VERSION_0_1 = "0.0.1";
    private static final String FIRMWARE_VERSION_1 = "1.0.0";

    private static final String HARDWARE_VERSION_A = "A";

    private static final String TEST_MODEL = "testModel";

    private ThingTypeUID thingTypeUID;

    private FirmwareProvider lambdaFirmwareProvider;

    @Before
    public void setup() {
        thingTypeUID = new ThingTypeUID("customBinding:type");
    }

    @After
    public void tearDown() {
        unregisterSingleFirmwareProvider();
    }

    @Test
    public void firmwareRestrictionExactFirmwareVersion() {
        FirmwareRestriction firmwareRestrictionFunction = t -> t.getProperties().get(Thing.PROPERTY_FIRMWARE_VERSION)
                .equals(FIRMWARE_VERSION_1);

        Firmware firmware = FirmwareBuilder.create(thingTypeUID, FIRMWARE_VERSION_2)
                .withFirmwareRestriction(firmwareRestrictionFunction).build();

        registerFirmwareProviderWithSingleFirmware(firmware);

        assertThatFirmwareUpdateIsExecutableForThingWithFirmwareVersionAndHardwareVersion(FIRMWARE_VERSION_1, null);
    }

    @Test
    public void firmwareRestrictionExactHardwareVersion() {
        FirmwareRestriction hardwareRestrictionFunction = t -> t.getProperties().get(Thing.PROPERTY_HARDWARE_VERSION)
                .equals(HARDWARE_VERSION_A);

        Firmware firmware = FirmwareBuilder.create(thingTypeUID, FIRMWARE_VERSION_2)
                .withFirmwareRestriction(hardwareRestrictionFunction).build();

        registerFirmwareProviderWithSingleFirmware(firmware);

        assertThatFirmwareUpdateIsExecutableForThingWithFirmwareVersionAndHardwareVersion(FIRMWARE_VERSION_0_1,
                HARDWARE_VERSION_A);
    }

    @Test
    public void firmwareRestrictionExactFirmwareAndHardwareVersion() {
        FirmwareRestriction fwAndHwRestrictionFunction = t -> t.getProperties().get(Thing.PROPERTY_HARDWARE_VERSION)
                .equals(HARDWARE_VERSION_A)
                && t.getProperties().get(Thing.PROPERTY_FIRMWARE_VERSION).equals(FIRMWARE_VERSION_1);

        Firmware firmware = FirmwareBuilder.create(thingTypeUID, FIRMWARE_VERSION_2)
                .withFirmwareRestriction(fwAndHwRestrictionFunction).build();

        registerFirmwareProviderWithSingleFirmware(firmware);

        assertThatFirmwareUpdateIsExecutableForThingWithFirmwareVersionAndHardwareVersion(FIRMWARE_VERSION_1,
                HARDWARE_VERSION_A);
    }

    @Test
    public void firmwareRestrictionAndRestrictedModel() {
        FirmwareRestriction firmwareRestrictionFunction = t -> t.getProperties().get(Thing.PROPERTY_FIRMWARE_VERSION)
                .equals(FIRMWARE_VERSION_1);

        Firmware firmware = FirmwareBuilder.create(thingTypeUID, FIRMWARE_VERSION_2).withModelRestricted(true)
                .withModel(TEST_MODEL).withFirmwareRestriction(firmwareRestrictionFunction).build();

        registerFirmwareProviderWithSingleFirmware(firmware);

        assertThatFirmwareUpdateIsExecutableForThingWithFirmwareVersionAndHardwareVersion(FIRMWARE_VERSION_1, null);
    }

    @Test
    public void firmwareRestrictionAndPrerequisiteVersion() {
        FirmwareRestriction firmwareRestrictionFunction = t -> t.getProperties().get(Thing.PROPERTY_FIRMWARE_VERSION)
                .equals(FIRMWARE_VERSION_1);

        Firmware firmware = FirmwareBuilder.create(thingTypeUID, FIRMWARE_VERSION_2).withPrerequisiteVersion("0.0.5")
                .withFirmwareRestriction(firmwareRestrictionFunction).build();

        registerFirmwareProviderWithSingleFirmware(firmware);

        assertThatFirmwareUpdateIsExecutableForThingWithFirmwareVersionAndHardwareVersion(FIRMWARE_VERSION_1, null);
    }

    @Test
    public void hardwareVersionDependsOnInstalledFirmware() {
        // Hardware version 1 things
        Thing hwVersionOneLowFwVersionThing = createThing("hw-version-1-low", "12", "0");
        registerService(createFirmwareUpdateHandler(hwVersionOneLowFwVersionThing));
        Thing hwVersionOneHighFwVersionThing = createThing("hw-version-1-high", FW_VERSION_32, "0");
        registerService(createFirmwareUpdateHandler(hwVersionOneHighFwVersionThing));

        // Hardware version 2 things
        Thing hwVersionTwoLowFwVersionThing = createThing("hw-version-2-low", "19", "0");
        registerService(createFirmwareUpdateHandler(hwVersionTwoLowFwVersionThing));
        Thing hwVersionTwoHighFwVersionThing = createThing("hw-version-2-high", "31", "0");
        registerService(createFirmwareUpdateHandler(hwVersionTwoHighFwVersionThing));

        // Define restrictions for the hardware versions
        FirmwareRestriction hwVersion1Restriction = thg -> Integer
                .parseInt(thg.getProperties().get(Thing.PROPERTY_FIRMWARE_VERSION)) < 14
                || thg.getProperties().get(Thing.PROPERTY_FIRMWARE_VERSION).equals(FW_VERSION_32);

        FirmwareRestriction hwVersion2Restriction = thg -> Integer
                .parseInt(thg.getProperties().get(Thing.PROPERTY_FIRMWARE_VERSION)) >= 14
                && !thg.getProperties().get(Thing.PROPERTY_FIRMWARE_VERSION).equals(FW_VERSION_32);

        // Build firmwares
        Firmware fw38 = FirmwareBuilder.create(thingTypeUID, FW_VERSION_38)
                .withFirmwareRestriction(hwVersion2Restriction).build();
        Firmware fw40 = FirmwareBuilder.create(thingTypeUID, FW_VERSION_40)
                .withFirmwareRestriction(hwVersion1Restriction).build();

        // Mock the provider to return the firmwares
        FirmwareProvider lambdaFirmwareProvider = mock(FirmwareProvider.class);
        Set<Firmware> resultSet = new HashSet<>(Arrays.asList(fw38, fw40));
        when(lambdaFirmwareProvider.getFirmwares(any(), any())).thenReturn(resultSet);
        registerService(lambdaFirmwareProvider);

        FirmwareUpdateService firmwareUpdateService = getService(FirmwareUpdateService.class);

        assertCorrectStatusInfo(firmwareUpdateService, hwVersionOneLowFwVersionThing, FirmwareStatus.UPDATE_EXECUTABLE,
                FW_VERSION_40);

        assertCorrectStatusInfo(firmwareUpdateService, hwVersionOneHighFwVersionThing, FirmwareStatus.UPDATE_EXECUTABLE,
                FW_VERSION_40);

        assertCorrectStatusInfo(firmwareUpdateService, hwVersionTwoLowFwVersionThing, FirmwareStatus.UPDATE_EXECUTABLE,
                FW_VERSION_38);

        assertCorrectStatusInfo(firmwareUpdateService, hwVersionTwoHighFwVersionThing, FirmwareStatus.UPDATE_EXECUTABLE,
                FW_VERSION_38);
    }

    private void assertThatFirmwareUpdateIsExecutableForThingWithFirmwareVersionAndHardwareVersion(
            String firmwareVersion, String hardwareVersion) {
        Thing thing = createThing("customThing", firmwareVersion, hardwareVersion);
        registerService(createFirmwareUpdateHandler(thing));

        FirmwareUpdateService firmwareUpdateService = getService(FirmwareUpdateService.class);

        FirmwareStatusInfo firmwareStatusInfo = firmwareUpdateService.getFirmwareStatusInfo(thing.getUID());

        assertThat(firmwareStatusInfo, notNullValue());
        assertThat(firmwareStatusInfo.getFirmwareStatus(), is(FirmwareStatus.UPDATE_EXECUTABLE));
    }

    private void assertCorrectStatusInfo(FirmwareUpdateService firmwareUpdateService, Thing thing,
            FirmwareStatus expectedStatus, String expectedFirmwareVersion) {
        FirmwareStatusInfo firmwareStatusInfo = firmwareUpdateService.getFirmwareStatusInfo(thing.getUID());

        assertThat(firmwareStatusInfo, notNullValue());
        assertThat(firmwareStatusInfo.getFirmwareStatus(), is(expectedStatus));
        assertThat(firmwareStatusInfo.getUpdatableFirmwareVersion(), is(expectedFirmwareVersion));
    }

    private Thing createThing(String thingUID, String firmwareVersion, String hardwareVersion) {
        Thing thing = mock(Thing.class);

        when(thing.getThingTypeUID()).thenReturn(thingTypeUID);
        when(thing.getUID()).thenReturn(new ThingUID(thingTypeUID, thingUID));

        Map<String, String> propertiesMap = new HashMap<>();
        if (firmwareVersion != null) {
            propertiesMap.put(Thing.PROPERTY_FIRMWARE_VERSION, firmwareVersion);
        }
        if (hardwareVersion != null) {
            propertiesMap.put(Thing.PROPERTY_HARDWARE_VERSION, hardwareVersion);
        }

        propertiesMap.put(Thing.PROPERTY_MODEL_ID, TEST_MODEL);
        when(thing.getProperties()).thenReturn(propertiesMap);

        return thing;
    }

    private FirmwareUpdateHandler createFirmwareUpdateHandler(Thing thing) {
        FirmwareUpdateHandler firmwareUpdateHandler = mock(FirmwareUpdateHandler.class);
        when(firmwareUpdateHandler.getThing()).thenReturn(thing);
        when(firmwareUpdateHandler.isUpdateExecutable()).thenReturn(true);
        return firmwareUpdateHandler;
    }

    private void registerFirmwareProviderWithSingleFirmware(Firmware firmwareToReturn) {
        lambdaFirmwareProvider = mock(FirmwareProvider.class);
        Set<Firmware> resultSet = new HashSet<>(Arrays.asList(firmwareToReturn));
        when(lambdaFirmwareProvider.getFirmwares(any(), any())).thenReturn(resultSet);
        when(lambdaFirmwareProvider.getFirmware(any(), any(), any())).thenReturn(firmwareToReturn);
        registerService(lambdaFirmwareProvider);
    }

    private void unregisterSingleFirmwareProvider() {
        if (lambdaFirmwareProvider != null) {
            unregisterService(lambdaFirmwareProvider);
        }
    }
}
