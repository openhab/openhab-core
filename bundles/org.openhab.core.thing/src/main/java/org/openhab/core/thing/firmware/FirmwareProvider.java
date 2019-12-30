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

import java.util.Locale;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.binding.firmware.Firmware;

/**
 * The {@link FirmwareProvider} is registered as an OSGi service and is responsible for providing firmwares. If a locale
 * is given to one of its operations then the following firmware attributes are to be localized:
 * <ul>
 * <li>{@link Firmware#getDescription()}</li>
 * <li>{@link Firmware#getChangelog()}</li>
 * <li>{@link Firmware#getOnlineChangelog()}</li>
 * <ul>
 *
 * @author Thomas Höfer - Initial contribution
 * @author Dimitar Ivanov - Firmwares are provided for thing
 */
@NonNullByDefault
public interface FirmwareProvider {

    /**
     * Returns the firmware for the given thing and provided firmware version.
     *
     * @param thing the thing for which the firmware will be provided with the specified version
     * @param version the version of the firmware to be provided for the specified thing
     * @return the corresponding firmware or <code>null</code> if no firmware was found
     */
    @Nullable
    Firmware getFirmware(Thing thing, String version);

    /**
     * Returns the firmware for the given thing and version for the given locale.
     *
     * @param thing the thing for which the firmwares are to be provided (not null)
     * @param version the version of the firmware to be provided
     * @param locale the locale to be used (if null then the default locale is to be used)
     * @return the corresponding firmware for the given locale or null if no firmware was found
     */
    @Nullable
    Firmware getFirmware(Thing thing, String version, @Nullable Locale locale);

    /**
     * Returns the set of available firmwares for the given thing.
     *
     * @param thing the thing for which the firmwares are to be provided (not null)
     * @return the set of available firmwares for the given thing (can be null)
     */
    @Nullable
    Set<Firmware> getFirmwares(Thing thing);

    /**
     * Returns the set of available firmwares for the given thing and the given locale.
     *
     * @param thing the thing for which the firmwares are to be provided (not null)
     * @param locale the locale to be used (if null then the default locale is to be used)
     * @return the set of available firmwares for the given thing (can be null)
     */
    @Nullable
    Set<Firmware> getFirmwares(Thing thing, @Nullable Locale locale);
}
