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

import java.util.Collection;
import java.util.Locale;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.i18n.LocaleProvider;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.binding.firmware.Firmware;

/**
 * The {@link FirmwareRegistry} is registered as an OSGi service and is responsible for tracking all
 * {@link FirmwareProvider}s. For this reason it is the central instance to get access to all available firmwares. If a
 * locale is given to one of its operations then the following firmware attributes are localized:
 * <ul>
 * <li>{@link Firmware#getDescription()}</li>
 * <li>{@link Firmware#getChangelog()}</li>
 * <li>{@link Firmware#getOnlineChangelog()}</li>
 * <ul>
 *
 * @author Thomas Höfer - Initial contribution
 * @author Dimitar Ivanov - Extracted interface
 */
@NonNullByDefault
public interface FirmwareRegistry {
    /**
     * Returns the firmware for the given thing and firmware version by using the locale provided by the
     * {@link LocaleProvider}.
     *
     * @param thing the thing for which the firmwares are to be retrieved (not null)
     * @param firmwareVersion the version of the firmware to be retrieved (not null)
     * @return the corresponding firmware or null if no firmware was found
     * @throws IllegalArgumentException if the thing is null; if the firmware version is null or empty
     */
    @Nullable
    Firmware getFirmware(Thing thing, String firmwareVersion);

    /**
     * Returns the firmware for the given thing, firmware version and locale.
     *
     * @param thing the thing for which the firmwares are to be retrieved (not null)
     * @param firmwareVersion the version of the firmware to be retrieved (not null)
     * @param locale the locale to be used (if null then the locale provided by the {@link LocaleProvider} is used)
     * @return the corresponding firmware or null if no firmware was found
     * @throws IllegalArgumentException if the thing is null; if the firmware version is null or empty
     */
    @Nullable
    Firmware getFirmware(Thing thing, String firmwareVersion, Locale locale);

    /**
     * Returns the latest firmware for the given thing, using the locale provided by the {@link LocaleProvider}.
     *
     * @param thing the thing for which the firmwares are to be retrieved (not null)
     * @return the corresponding latest firmware or null if no firmware was found
     * @throws IllegalArgumentException if the thing is null
     */
    @Nullable
    Firmware getLatestFirmware(Thing thing);

    /**
     * Returns the latest firmware for the given thing and locale.
     *
     * @param thing the thing for which the firmwares are to be retrieved (not null)
     * @param locale the locale to be used (if null then the locale provided by the {@link LocaleProvider} is used)
     * @return the corresponding latest firmware or null if no firmware was found
     * @throws IllegalArgumentException if the thing is null
     */
    @Nullable
    Firmware getLatestFirmware(Thing thing, @Nullable Locale locale);

    /**
     * Returns the collection of available firmwares for the given thing using the locale provided by the
     * {@link LocaleProvider}. The collection is sorted in descending order, i.e. the latest firmware will be the first
     * element in the collection.
     *
     * @param thing the thing for which the firmwares are to be retrieved (not null)
     * @return the collection of available firmwares for the given thing (not null)
     * @throws IllegalArgumentException if the thing is null
     */
    Collection<Firmware> getFirmwares(Thing thing);

    /**
     * Returns the collection of available firmwares for the given thing and locale. The collection is
     * sorted in descending order, i.e. the latest firmware will be the first element in the collection.
     *
     * @param thing the thing for which the firmwares are to be retrieved (not null)
     * @param locale the locale to be used (if null then the locale provided by the {@link LocaleProvider} is used)
     * @return the collection of available firmwares for the given thing (not null)
     * @throws IllegalArgumentException if the thing is null
     */
    Collection<Firmware> getFirmwares(Thing thing, @Nullable Locale locale);
}
