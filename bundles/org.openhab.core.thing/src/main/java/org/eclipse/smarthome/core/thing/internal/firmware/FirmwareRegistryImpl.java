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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.i18n.LocaleProvider;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.binding.firmware.Firmware;
import org.eclipse.smarthome.core.thing.firmware.FirmwareProvider;
import org.eclipse.smarthome.core.thing.firmware.FirmwareRegistry;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of {@link FirmwareRegistry}.
 *
 * @author Thomas Höfer - Initial contribution
 * @author Dimitar Ivanov - The firmwares are provided by thing and version
 */
@Component(immediate = true, service = FirmwareRegistry.class)
@NonNullByDefault()
public final class FirmwareRegistryImpl implements FirmwareRegistry {

    private final Logger logger = LoggerFactory.getLogger(FirmwareRegistryImpl.class);

    private final List<FirmwareProvider> firmwareProviders = new CopyOnWriteArrayList<>();

    private @NonNullByDefault({}) LocaleProvider localeProvider;

    @Override
    @Nullable
    public Firmware getFirmware(Thing thing, String firmwareVersion) {
        return getFirmware(thing, firmwareVersion, localeProvider.getLocale());
    }

    @Override
    @Nullable
    public Firmware getFirmware(Thing thing, String firmwareVersion, @Nullable Locale locale) {
        ParameterChecks.checkNotNull(thing, "Thing");
        ParameterChecks.checkNotNullOrEmpty(firmwareVersion, "Firmware version");

        Locale loc = locale != null ? locale : localeProvider.getLocale();

        for (FirmwareProvider firmwareProvider : firmwareProviders) {
            try {
                Firmware firmware = firmwareProvider.getFirmware(thing, firmwareVersion, loc);
                if (firmware != null && firmware.isSuitableFor(thing)) {
                    return firmware;
                }
            } catch (Exception e) {
                logger.warn(
                        "Unexpected exception occurred for firmware provider {} while getting firmware with version {} for thing {}",
                        firmwareProvider.getClass().getSimpleName(), firmwareVersion, thing.getThingTypeUID(), e);
            }
        }

        return null;
    }

    @Override
    @Nullable
    public Firmware getLatestFirmware(Thing thing) {
        return getLatestFirmware(thing, localeProvider.getLocale());
    }

    @Override
    @Nullable
    public Firmware getLatestFirmware(Thing thing, @Nullable Locale locale) {
        Locale loc = locale != null ? locale : localeProvider.getLocale();
        Collection<Firmware> firmwares = getFirmwares(thing, loc);

        if (firmwares != null) {
            Optional<Firmware> first = firmwares.stream().findFirst();

            // Used as workaround for the NonNull annotation implied to .isElse()
            if (first.isPresent()) {
                return first.get();
            }
        }

        return null;
    }

    @Override
    public Collection<Firmware> getFirmwares(Thing thing) {
        return getFirmwares(thing, localeProvider.getLocale());
    }

    @Override
    public Collection<Firmware> getFirmwares(Thing thing, @Nullable Locale locale) {
        ParameterChecks.checkNotNull(thing, "Thing");

        Locale loc = locale != null ? locale : localeProvider.getLocale();

        Set<Firmware> firmwares = new TreeSet<>();
        for (FirmwareProvider firmwareProvider : firmwareProviders) {
            try {
                Collection<Firmware> result = firmwareProvider.getFirmwares(thing, loc);
                if (result != null) {
                    List<Firmware> suitableFirmwares = result.stream().filter(firmware -> firmware.isSuitableFor(thing))
                            .collect(Collectors.toList());
                    firmwares.addAll(suitableFirmwares);
                }
            } catch (Exception e) {
                logger.warn(
                        "Unexpected exception occurred for firmware provider {} while getting firmwares for thing {}.",
                        firmwareProvider.getClass().getSimpleName(), thing.getUID(), e);
            }
        }

        return Collections.unmodifiableCollection(firmwares);
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addFirmwareProvider(FirmwareProvider firmwareProvider) {
        firmwareProviders.add(firmwareProvider);
    }

    protected void removeFirmwareProvider(FirmwareProvider firmwareProvider) {
        firmwareProviders.remove(firmwareProvider);
    }

    @Reference
    protected void setLocaleProvider(final LocaleProvider localeProvider) {
        this.localeProvider = localeProvider;
    }

    protected void unsetLocaleProvider(final LocaleProvider localeProvider) {
        this.localeProvider = null;
    }
}
