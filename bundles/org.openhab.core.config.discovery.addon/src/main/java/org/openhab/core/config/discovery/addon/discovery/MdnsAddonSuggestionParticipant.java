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

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.jmdns.ServiceInfo;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.discovery.DiscoveryResult;
import org.openhab.core.config.discovery.addon.finder.AddonSuggestionListener;
import org.openhab.core.config.discovery.mdns.MDNSDiscoveryParticipant;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.osgi.service.component.annotations.Component;

/**
 * This is a {@link MdnsAddonSuggestionParticipant} mdns version of class for
 * discovery participants to find suggested addons.
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
@NonNullByDefault
@Component(service = MDNSDiscoveryParticipant.class, configurationPid = "discovery.mdns.suggestion.finder")
public class MdnsAddonSuggestionParticipant extends AddonSuggestionParticipant implements MDNSDiscoveryParticipant {
    private final String serviceType;

    public MdnsAddonSuggestionParticipant(AddonSuggestionListener listener, String bindingId,
            Map<String, String> propertyMatchRegex, String serviceType) {
        super(listener, bindingId, propertyMatchRegex);
        this.serviceType = serviceType;
    }

    @Override
    public Set<ThingTypeUID> getSupportedThingTypeUIDs() {
        return Set.of();
    }

    @Override
    public String getServiceType() {
        return serviceType;
    }

    @Override
    public @Nullable DiscoveryResult createResult(ServiceInfo serviceInfo) {
        if (isPropertyValid("application", serviceInfo.getApplication())
                && isPropertyValid("name", serviceInfo.getName())
                && Collections.list(serviceInfo.getPropertyNames()).stream().allMatch(
                        propertyName -> isPropertyValid(propertyName, serviceInfo.getPropertyString(propertyName)))) {
            listener.onAddonSuggestionFound(bindingId);
        }
        return null;
    }

    @Override
    public @Nullable ThingUID getThingUID(ServiceInfo service) {
        return null;
    }
}
