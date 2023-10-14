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

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.addon.AddonDiscoveryMethod;
import org.openhab.core.addon.AddonDiscoveryServiceType;
import org.openhab.core.addon.AddonInfo;
import org.openhab.core.io.transport.mdns.MDNSClient;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * This is a {@link MDNSAddonSuggestionFinder} for finding suggested Addons via MDNS.
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
@NonNullByDefault
@Component(service = AddonSuggestionFinder.class, name = MDNSAddonSuggestionFinder.SERVICE_NAME)
public class MDNSAddonSuggestionFinder extends BaseAddonSuggestionFinder {

    public static final String SERVICE_NAME = "mdns-addon-suggestion-finder";

    /**
     * Anonymous ServiceListener implementation that ignores call-backs.
     */
    private final ServiceListener noOp = new ServiceListener() {

        @Override
        public void serviceAdded(@Nullable ServiceEvent event) {
        }

        @Override
        public void serviceRemoved(@Nullable ServiceEvent event) {
        }

        @Override
        public void serviceResolved(@Nullable ServiceEvent event) {
        }
    };

    private final MDNSClient mdnsClient;

    @Activate
    public MDNSAddonSuggestionFinder(@Reference MDNSClient mdnsClient) {
        this.mdnsClient = mdnsClient;
    }

    @Override
    public void scanTask() {
        for (AddonInfo candidate : addonCandidates) {
            for (AddonDiscoveryMethod method : candidate.getDiscoveryMethods()) {
                if (AddonDiscoveryServiceType.MDNS != method.getServiceType()) {
                    continue;
                }
                Map<String, String> map = method.getPropertyRegexMap();
                for (ServiceInfo service : mdnsClient.list(method.getMdnsServiceType())) {
                    if (Thread.interrupted()) {
                        // using nested for loops instead of forEach to allow external interruption
                        return;
                    }
                    if (propertyMatches(map, "application", service.getApplication())
                            && propertyMatches(map, "name", service.getName())
                            && Collections.list(service.getPropertyNames()).stream()
                                    .allMatch(n -> propertyMatches(map, n, service.getPropertyString(n)))) {
                        addonSuggestionUIDs.add(candidate.getUID());
                    }
                }
            }
        }
        scanDone = true;
    }

    @Override
    public void setAddonCandidates(List<AddonInfo> candidates) {
        super.setAddonCandidates(candidates);
        addonCandidates.forEach(
                c -> c.getDiscoveryMethods().stream().filter(m -> AddonDiscoveryServiceType.MDNS == m.getServiceType())
                        .forEach(m -> mdnsClient.addServiceListener(m.getMdnsServiceType(), noOp)));
    }
}
