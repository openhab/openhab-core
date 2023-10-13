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
package org.openhab.core.config.discovery.addon.finders;

import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.jupnp.UpnpService;
import org.openhab.core.addon.AddonDiscoveryServiceType;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * This is a {@link UpnpAddonSuggestionFinder} for finding suggested addons via
 * UPnP.
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
@NonNullByDefault
@Component(service = AddonSuggestionFinder.class, name = UpnpAddonSuggestionFinder.SERVICE_NAME)
public class UpnpAddonSuggestionFinder extends AddonSuggestionFinder {

    public static final String SERVICE_NAME = "upnp-addon-suggestion-finder";

    private final UpnpService upnpService;

    @Activate
    public UpnpAddonSuggestionFinder(@Reference UpnpService upnpService) {
        this.upnpService = upnpService;
    }

    @Override
    public void scanTask() {
        upnpService.getRegistry().getRemoteDevices().forEach(d -> {
            addonCandidates.forEach(c -> {
                c.getDiscoveryMethods().stream().filter(m -> AddonDiscoveryServiceType.UPNP == m.getServiceType())
                        .forEach(m -> {
                            Map<String, String> map = m.getPropertyRegexMap();
                            if (propertyMatches(map, "deviceType", d.getType().getType())
                                    && propertyMatches(map, "manufacturer",
                                            d.getDetails().getManufacturerDetails().getManufacturer())
                                    && propertyMatches(map, "modelName",
                                            d.getDetails().getModelDetails().getModelName())
                                    && propertyMatches(map, "serialNumber", d.getDetails().getSerialNumber())
                                    && propertyMatches(map, "udn", d.getIdentity().getUdn().getIdentifierString())) {
                                addonSuggestionUIDs.add(c.getUID());
                            }
                        });
            });
        });
        scanDone = true;
    }
}
