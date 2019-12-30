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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.openhab.core.test.java.JavaOSGiTest;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.firmware.Firmware;
import org.openhab.core.thing.binding.firmware.FirmwareBuilder;
import org.openhab.core.thing.binding.firmware.FirmwareUpdateHandler;

/**
 * Tests that the {@link FirmwareUpdateService} honors the prerequisite version of {@link Firmware}s.
 *
 * @author Henning Sudbrock - Initial contribution
 */
public class FirmwareUpdateServicePrerequisiteVersionTest extends JavaOSGiTest {

    private ThingTypeUID thingTypeUID;

    private Firmware firmwareV1;
    private Firmware firmwareV2;
    private Firmware firmwareV3;

    private Thing thingWithFirmwareV1;
    private Thing thingWithFirmwareV2;

    @Before
    public void setup() {
        thingTypeUID = new ThingTypeUID("binding:thingtype");

        firmwareV1 = FirmwareBuilder.create(thingTypeUID, "1.0.0").build();
        firmwareV2 = FirmwareBuilder.create(thingTypeUID, "2.0.0").build();
        firmwareV3 = FirmwareBuilder.create(thingTypeUID, "3.0.0").withPrerequisiteVersion("2.0.0").build();

        thingWithFirmwareV1 = createThing(thingTypeUID, "thingWithFirmwareV1", firmwareV1);
        thingWithFirmwareV2 = createThing(thingTypeUID, "thingWithFirmwareV2", firmwareV2);

        registerService(createFirmwareProvider(thingWithFirmwareV1, firmwareV1, firmwareV2, firmwareV3));
        registerService(createFirmwareProvider(thingWithFirmwareV2, firmwareV1, firmwareV2, firmwareV3));
        registerService(createFirmwareUpdateHandler(thingWithFirmwareV1));
        registerService(createFirmwareUpdateHandler(thingWithFirmwareV2));
    }

    @SuppressWarnings("null")
    @Test
    public void testGetFirmwareStatusInfoWithUnsatisfiedPrerequisiteVersion() {
        FirmwareUpdateService firmwareUpdateService = getService(FirmwareUpdateService.class);

        FirmwareStatusInfo firmwareStatusInfo = firmwareUpdateService
                .getFirmwareStatusInfo(thingWithFirmwareV1.getUID());
        assertThat(firmwareStatusInfo, notNullValue());

        // Since firmware version 3.0.0 has prerequisite version 2.0.0, and the thing has firmware version 1.0.0, the
        // expectation is that the version of the firmware to which one can update is 2.0.0.
        assertThat(firmwareStatusInfo.getUpdatableFirmwareVersion(), equalTo("2.0.0"));
    }

    @SuppressWarnings("null")
    @Test
    public void testGetFirmwareStatusInfoWithSatisfiedPrerequisiteVersion() {
        FirmwareUpdateService firmwareUpdateService = getService(FirmwareUpdateService.class);

        FirmwareStatusInfo firmwareStatusInfo = firmwareUpdateService
                .getFirmwareStatusInfo(thingWithFirmwareV2.getUID());
        assertThat(firmwareStatusInfo, notNullValue());

        // Since firmware version 3.0.0 has prerequisite version 2.0.0, and the thing has firmware version 2.0.0, the
        // expectation is that the version of the firmware to which one can update is 3.0.0.
        assertThat(firmwareStatusInfo.getUpdatableFirmwareVersion(), equalTo("3.0.0"));
    }

    private Thing createThing(ThingTypeUID thingTypeUID, String thingUID, Firmware firmware) {
        Thing thing = mock(Thing.class);

        when(thing.getThingTypeUID()).thenReturn(thingTypeUID);
        when(thing.getUID()).thenReturn(new ThingUID(thingTypeUID, thingUID));

        Map<String, String> propertiesMap = new HashMap<>();
        propertiesMap.put(Thing.PROPERTY_FIRMWARE_VERSION, firmware.getVersion());
        when(thing.getProperties()).thenReturn(propertiesMap);

        return thing;
    }

    private FirmwareProvider createFirmwareProvider(Thing thing, Firmware... firmwares) {
        FirmwareProvider firmwareProvider = mock(FirmwareProvider.class);
        when(firmwareProvider.getFirmwares(eq(thing), any(Locale.class)))
                .thenReturn(new HashSet<>(Arrays.asList(firmwares)));
        return firmwareProvider;
    }

    private FirmwareUpdateHandler createFirmwareUpdateHandler(Thing thing) {
        FirmwareUpdateHandler firmwareUpdateHandler = mock(FirmwareUpdateHandler.class);
        when(firmwareUpdateHandler.getThing()).thenReturn(thing);
        when(firmwareUpdateHandler.isUpdateExecutable()).thenReturn(true);
        return firmwareUpdateHandler;
    }

}
