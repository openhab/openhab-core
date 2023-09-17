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
package org.openhab.core.config.discovery.addon.discovery;

import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.jupnp.model.meta.RemoteDevice;
import org.openhab.core.config.discovery.DiscoveryResult;
import org.openhab.core.config.discovery.addon.finder.AddonSuggestionListener;
import org.openhab.core.config.discovery.upnp.UpnpDiscoveryParticipant;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.osgi.service.component.annotations.Component;

/**
 * This is a {@link UpnpAddonSuggestionParticipant} upnp version of class for
 * discovery participants to find suggested addons.
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
@NonNullByDefault
@Component(service = UpnpDiscoveryParticipant.class, configurationPid = "discovery.upnp.suggestion.finder")
public class UpnpAddonSuggestionParticipant extends AddonSuggestionParticipant implements UpnpDiscoveryParticipant {

    public UpnpAddonSuggestionParticipant(AddonSuggestionListener listener, String bindingId,
            Map<String, String> propertyMatchRegexMap) {
        super(listener, bindingId, propertyMatchRegexMap);
    }

    @Override
    public Set<ThingTypeUID> getSupportedThingTypeUIDs() {
        return Set.of();
    }

    @Override
    public @Nullable DiscoveryResult createResult(RemoteDevice device) {
        if (isPropertyValid("deviceType", device.getType().getType())
                && isPropertyValid("manufacturer", device.getDetails().getManufacturerDetails().getManufacturer())
                && isPropertyValid("model", device.getDetails().getModelDetails().getModelName())
                && isPropertyValid("serialNumber", device.getDetails().getSerialNumber())
                && isPropertyValid("udn", device.getIdentity().getUdn().getIdentifierString())) {
            listener.onAddonSuggestionFound(bindingId);
        }
        return null;
    }

    @Override
    public @Nullable ThingUID getThingUID(RemoteDevice device) {
        return null;
    }
}
