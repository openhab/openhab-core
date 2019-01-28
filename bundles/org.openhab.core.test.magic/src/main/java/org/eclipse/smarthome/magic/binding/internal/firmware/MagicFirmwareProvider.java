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
package org.eclipse.smarthome.magic.binding.internal.firmware;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.binding.firmware.Firmware;
import org.eclipse.smarthome.core.thing.binding.firmware.FirmwareBuilder;
import org.eclipse.smarthome.core.thing.firmware.FirmwareProvider;
import org.eclipse.smarthome.magic.binding.MagicBindingConstants;
import org.osgi.service.component.annotations.Component;

/**
 * Provides firmware for the magic thing type for firmware update.
 *
 * @author Dimitar Ivanov - Initial contribution
 */
@Component(service = FirmwareProvider.class)
public class MagicFirmwareProvider implements FirmwareProvider {

    //@formatter:off
    private final Set<Firmware> magicFirmwares = Stream
            // General firmware versions for the thing type
            .of(createFirmware(null, "0.1.0", false),
                createFirmware(null, "1.0.0", false),
             // Model restricted firmware versions
                createFirmware(MagicBindingConstants.MODEL_ALOHOMORA, "1.0.1", true),
                createFirmware(MagicBindingConstants.MODEL_ALOHOMORA, "1.1.0", true),
                createFirmware(MagicBindingConstants.MODEL_COLLOPORTUS, "1.0.1", true),
                createFirmware(MagicBindingConstants.MODEL_COLLOPORTUS, "1.2.0", true),
                createFirmware(MagicBindingConstants.MODEL_LUMOS, "2.3.1", true),
                createFirmware(MagicBindingConstants.MODEL_LUMOS, "2.5.0", true)
                ).collect(Collectors.toSet());
    //@formatter:on

    @Override
    public Firmware getFirmware(Thing thing, String version) {
        return getFirmware(thing, version, null);
    }

    @SuppressWarnings("null")
    @Override
    public Firmware getFirmware(Thing thing, String version, Locale locale) {
        return getFirmwares(thing, locale).stream().filter(firmware -> firmware.getVersion().equals(version))
                .findFirst().get();
    }

    @Override
    public Set<Firmware> getFirmwares(Thing thing) {
        return getFirmwares(thing, null);
    }

    @Override
    public Set<Firmware> getFirmwares(Thing thing, Locale locale) {
        return magicFirmwares.stream().filter(firmware -> firmware.isSuitableFor(thing)).collect(Collectors.toSet());
    }

    private static Firmware createFirmware(final String model, final String version, boolean modelRestricted) {
        Firmware firmware = FirmwareBuilder.create(MagicBindingConstants.THING_TYPE_FIRMWARE_UPDATE, version)
                .withModel(model).withModelRestricted(modelRestricted).build();
        return firmware;
    }
}
