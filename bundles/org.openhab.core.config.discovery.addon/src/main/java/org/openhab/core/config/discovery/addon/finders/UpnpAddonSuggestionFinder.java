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
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.jupnp.UpnpService;
import org.jupnp.model.meta.RemoteDevice;
import org.openhab.core.addon.AddonDiscoveryMethod;
import org.openhab.core.addon.AddonDiscoveryServiceType;
import org.openhab.core.addon.AddonInfo;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * This is a {@link UpnpAddonSuggestionFinder} for finding suggested Addons via UPnP.
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
@NonNullByDefault
@Component(service = AddonSuggestionFinder.class, name = UpnpAddonSuggestionFinder.SERVICE_NAME)
public class UpnpAddonSuggestionFinder extends BaseAddonSuggestionFinder {

    public static final String SERVICE_NAME = "upnp-addon-suggestion-finder";

    private final UpnpService upnpService;

    @Activate
    public UpnpAddonSuggestionFinder(@Reference UpnpService upnpService) {
        this.upnpService = upnpService;
    }

    @Override
    public void scanTask() {
        for (RemoteDevice device : upnpService.getRegistry().getRemoteDevices()) {
            for (AddonInfo candidate : addonCandidates) {
                for (AddonDiscoveryMethod method : candidate.getDiscoveryMethods()) {
                    if (Thread.interrupted()) {
                        // using nested for loops instead of forEach to allow external interruption
                        return;
                    }
                    if (AddonDiscoveryServiceType.UPNP != method.getServiceType()) {
                        continue;
                    }
                    Map<String, String> map = method.getMatchProperties().stream()
                            .collect(Collectors.toMap(e -> e.getName(), e -> e.getRegex()));
                    if (propertyMatches(map, "deviceType", device.getType().getType())
                            && propertyMatches(map, "manufacturer",
                                    device.getDetails().getManufacturerDetails().getManufacturer())
                            && propertyMatches(map, "modelName", device.getDetails().getModelDetails().getModelName())
                            && propertyMatches(map, "serialNumber", device.getDetails().getSerialNumber())
                            && propertyMatches(map, "udn", device.getIdentity().getUdn().getIdentifierString())) {
                        addonSuggestionUIDs.add(candidate.getUID());
                    }
                }
            }
        }
        scanDone = true;
    }
}
