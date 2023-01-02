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
package org.openhab.core.thing.firmware;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
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
@NonNullByDefault
public class FirmwareUpdateServiceFirmwareRestrictionTest extends JavaOSGiTest {
    private static final String FW_VERSION_32 = "32";
    private static final String FW_VERSION_40 = "40";
    private static final String FW_VERSION_38 = "38";
    private static final String FIRMWARE_VERSION_2 = "2.0.0";
    private static final String FIRMWARE_VERSION_0_1 = "0.0.1";
    private static final String FIRMWARE_VERSION_1 = "1.0.0";
    private static final String HARDWARE_VERSION_A = "A";
    private static final String TEST_MODEL = "testModel";
    private static final ThingTypeUID THING_TYPE_UID = new ThingTypeUID("customBinding:type");

    private @Nullable FirmwareProvider lambdaFirmwareProvider;

    @AfterEach
    public void tearDown() {
        unregisterSingleFirmwareProvider();
    }

    @Test
    public void firmwareRestrictionExactFirmwareVersion() {
        FirmwareRestriction firmwareRestrictionFunction = t -> FIRMWARE_VERSION_1
                .equals(t.getProperties().get(Thing.PROPERTY_FIRMWARE_VERSION));

        Firmware firmware = FirmwareBuilder.create(THING_TYPE_UID, FIRMWARE_VERSION_2)
                .withFirmwareRestriction(firmwareRestrictionFunction).build();

        registerFirmwareProviderWithSingleFirmware(firmware);

        assertThatFirmwareUpdateIsExecutableForThingWithFirmwareVersionAndHardwareVersion(FIRMWARE_VERSION_1, null);
    }

    @Test
    public void firmwareRestrictionExactHardwareVersion() {
        FirmwareRestriction hardwareRestrictionFunction = t -> HARDWARE_VERSION_A
                .equals(t.getProperties().get(Thing.PROPERTY_HARDWARE_VERSION));

        Firmware firmware = FirmwareBuilder.create(THING_TYPE_UID, FIRMWARE_VERSION_2)
                .withFirmwareRestriction(hardwareRestrictionFunction).build();

        registerFirmwareProviderWithSingleFirmware(firmware);

        assertThatFirmwareUpdateIsExecutableForThingWithFirmwareVersionAndHardwareVersion(FIRMWARE_VERSION_0_1,
                HARDWARE_VERSION_A);
    }

    @Test
    public void firmwareRestrictionExactFirmwareAndHardwareVersion() {
        FirmwareRestriction fwAndHwRestrictionFunction = t -> HARDWARE_VERSION_A
                .equals(t.getProperties().get(Thing.PROPERTY_HARDWARE_VERSION))
                && FIRMWARE_VERSION_1.equals(t.getProperties().get(Thing.PROPERTY_FIRMWARE_VERSION));

        Firmware firmware = FirmwareBuilder.create(THING_TYPE_UID, FIRMWARE_VERSION_2)
                .withFirmwareRestriction(fwAndHwRestrictionFunction).build();

        registerFirmwareProviderWithSingleFirmware(firmware);

        assertThatFirmwareUpdateIsExecutableForThingWithFirmwareVersionAndHardwareVersion(FIRMWARE_VERSION_1,
                HARDWARE_VERSION_A);
    }

    @Test
    public void firmwareRestrictionAndRestrictedModel() {
        FirmwareRestriction firmwareRestrictionFunction = t -> FIRMWARE_VERSION_1
                .equals(t.getProperties().get(Thing.PROPERTY_FIRMWARE_VERSION));

        Firmware firmware = FirmwareBuilder.create(THING_TYPE_UID, FIRMWARE_VERSION_2).withModelRestricted(true)
                .withModel(TEST_MODEL).withFirmwareRestriction(firmwareRestrictionFunction).build();

        registerFirmwareProviderWithSingleFirmware(firmware);

        assertThatFirmwareUpdateIsExecutableForThingWithFirmwareVersionAndHardwareVersion(FIRMWARE_VERSION_1, null);
    }

    @Test
    public void firmwareRestrictionAndPrerequisiteVersion() {
        FirmwareRestriction firmwareRestrictionFunction = t -> FIRMWARE_VERSION_1
                .equals(t.getProperties().get(Thing.PROPERTY_FIRMWARE_VERSION));

        Firmware firmware = FirmwareBuilder.create(THING_TYPE_UID, FIRMWARE_VERSION_2).withPrerequisiteVersion("0.0.5")
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
        FirmwareRestriction hwVersion1Restriction = thg -> {
            String version = thg.getProperties().get(Thing.PROPERTY_FIRMWARE_VERSION);
            return version != null && (Integer.parseInt(version) < 14 || FW_VERSION_32.equals(version));
        };

        FirmwareRestriction hwVersion2Restriction = thg -> {
            String version = thg.getProperties().get(Thing.PROPERTY_FIRMWARE_VERSION);
            return version != null && (Integer.parseInt(version) >= 14 && !FW_VERSION_32.equals(version));
        };

        // Build firmwares
        Firmware fw38 = FirmwareBuilder.create(THING_TYPE_UID, FW_VERSION_38)
                .withFirmwareRestriction(hwVersion2Restriction).build();
        Firmware fw40 = FirmwareBuilder.create(THING_TYPE_UID, FW_VERSION_40)
                .withFirmwareRestriction(hwVersion1Restriction).build();

        // Mock the provider to return the firmwares
        FirmwareProvider lambdaFirmwareProvider = mock(FirmwareProvider.class);
        when(lambdaFirmwareProvider.getFirmwares(any(), any())).thenReturn(Set.of(fw38, fw40));
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
            String firmwareVersion, @Nullable String hardwareVersion) {
        Thing thing = createThing("customThing", firmwareVersion, hardwareVersion);
        registerService(createFirmwareUpdateHandler(thing));

        FirmwareUpdateService firmwareUpdateService = getService(FirmwareUpdateService.class);

        FirmwareStatusInfo firmwareStatusInfo = firmwareUpdateService.getFirmwareStatusInfo(thing.getUID());

        assertThat(firmwareStatusInfo, notNullValue());
        assertThat(firmwareStatusInfo.getFirmwareStatus(), is(FirmwareStatus.UPDATE_EXECUTABLE));
    }

    private void assertCorrectStatusInfo(@Nullable FirmwareUpdateService firmwareUpdateService, Thing thing,
            FirmwareStatus expectedStatus, String expectedFirmwareVersion) {
        FirmwareStatusInfo firmwareStatusInfo = firmwareUpdateService.getFirmwareStatusInfo(thing.getUID());

        assertThat(firmwareStatusInfo, notNullValue());
        assertThat(firmwareStatusInfo.getFirmwareStatus(), is(expectedStatus));
        assertThat(firmwareStatusInfo.getUpdatableFirmwareVersion(), is(expectedFirmwareVersion));
    }

    private Thing createThing(String thingUID, @Nullable String firmwareVersion, @Nullable String hardwareVersion) {
        Thing thing = mock(Thing.class);

        when(thing.getThingTypeUID()).thenReturn(THING_TYPE_UID);
        when(thing.getUID()).thenReturn(new ThingUID(THING_TYPE_UID, thingUID));

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
        FirmwareProvider lambdaFirmwareProvider = mock(FirmwareProvider.class);
        when(lambdaFirmwareProvider.getFirmwares(any(), any())).thenReturn(Set.of(firmwareToReturn));
        when(lambdaFirmwareProvider.getFirmware(any(), any(), any())).thenReturn(firmwareToReturn);
        registerService(lambdaFirmwareProvider);

        this.lambdaFirmwareProvider = lambdaFirmwareProvider;
    }

    private void unregisterSingleFirmwareProvider() {
        FirmwareProvider lambdaFirmwareProvider = this.lambdaFirmwareProvider;
        if (lambdaFirmwareProvider != null) {
            unregisterService(lambdaFirmwareProvider);
        }
    }
}
