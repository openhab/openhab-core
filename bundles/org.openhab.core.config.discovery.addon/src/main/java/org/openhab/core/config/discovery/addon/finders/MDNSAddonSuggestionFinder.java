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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.addon.AddonInfo;
import org.openhab.core.io.transport.mdns.MDNSClient;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a {@link MDNSAddonSuggestionFinder} for finding suggested Addons via MDNS.
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
@NonNullByDefault
@Component(service = AddonSuggestionFinder.class, name = MDNSAddonSuggestionFinder.SERVICE_NAME)
public class MDNSAddonSuggestionFinder extends BaseAddonSuggestionFinder implements ServiceListener {

    public static final String SERVICE_TYPE = "mdns";
    public static final String SERVICE_NAME = SERVICE_TYPE + ADDON_SUGGESTION_FINDER;

    private static final String NAME = "name";

    private final Logger logger = LoggerFactory.getLogger(MDNSAddonSuggestionFinder.class);
    private final Map<String, ServiceInfo> services = new ConcurrentHashMap<>();
    private final MDNSClient mdnsClient;

    @Activate
    public MDNSAddonSuggestionFinder(@Reference MDNSClient mdnsClient) {
        this.mdnsClient = mdnsClient;
    }

    public void addService(@Nullable ServiceInfo service) {
        if (service != null) {
            String qualifiedName = service.getQualifiedName();
            services.put(qualifiedName, service);
            if (logger.isTraceEnabled()) {
                Map<String, String> properties = new HashMap<>();
                while (service.getPropertyNames().hasMoreElements()) {
                    String name = service.getPropertyNames().nextElement();
                    properties.put(name, service.getPropertyString(name));
                }
                logger.trace("mDNS service name:{}, properties:{}", qualifiedName, properties.toString());
            }
        }
    }

    @Deactivate
    public void close() {
        services.clear();
        super.close();
    }

    @Override
    public Set<AddonInfo> getSuggestedAddons() {
        Set<AddonInfo> result = new HashSet<>();
        addonCandidates.forEach(candidate -> {
            candidate.getDiscoveryMethods().stream().filter(method -> SERVICE_TYPE.equals(method.getServiceType()))
                    .forEach(method -> {
                        Map<String, Pattern> map = method.getMatchProperties().stream().collect(
                                Collectors.toMap(property -> property.getName(), property -> property.getPattern()));
                        services.values().stream().forEach(service -> {
                            if (method.getMdnsServiceType().equals(service.getType())
                                    && propertyMatches(map, NAME, service.getName())
                                    && Collections.list(service.getPropertyNames()).stream().allMatch(
                                            name -> propertyMatches(map, name, service.getPropertyString(name)))) {
                                result.add(candidate);
                                logger.debug("Addon '{}' will be suggested", candidate.getUID());
                            }
                        });
                    });
        });
        return result;
    }

    @Override
    public void serviceAdded(@Nullable ServiceEvent event) {
        if (event != null) {
            addService(event.getInfo());
        }
    }

    @Override
    public void serviceRemoved(@Nullable ServiceEvent event) {
    }

    @Override
    public void serviceResolved(@Nullable ServiceEvent event) {
        if (event != null) {
            addService(event.getInfo());
        }
    }

    @Override
    public void setAddonCandidates(List<AddonInfo> candidates) {
        super.setAddonCandidates(candidates);
        candidates.forEach(c -> c.getDiscoveryMethods().stream().filter(m -> SERVICE_TYPE.equals(m.getServiceType()))
                .forEach(m -> mdnsClient.addServiceListener(m.getMdnsServiceType(), this)));
    }
}
