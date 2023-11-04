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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.addon.AddonDiscoveryMethod;
import org.openhab.core.addon.AddonInfo;
import org.openhab.core.common.ThreadPoolManager;
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
    private static final String APPLICATION = "application";

    private final Logger logger = LoggerFactory.getLogger(MDNSAddonSuggestionFinder.class);
    private final ScheduledExecutorService scheduler = ThreadPoolManager.getScheduledPool(SERVICE_NAME);
    private final Map<String, ServiceInfo> services = new ConcurrentHashMap<>();
    private final MDNSClient mdnsClient;

    @Activate
    public MDNSAddonSuggestionFinder(@Reference MDNSClient mdnsClient) {
        this.mdnsClient = mdnsClient;
    }

    public void addService(ServiceInfo service, boolean isResolved) {
        String qualifiedName = service.getQualifiedName();
        if (isResolved || !services.containsKey(qualifiedName)) {
            services.put(qualifiedName, service);
            logger.trace("Added service: {}/{}", qualifiedName, service.getNiceTextString());
        }
    }

    @Deactivate
    @Override
    protected void deactivate() {
        services.clear();
        super.deactivate();
    }

    @Override
    public Set<AddonInfo> getSuggestedAddons() {
        Set<AddonInfo> result = new HashSet<>();
        for (AddonInfo candidate : addonCandidates) {
            for (AddonDiscoveryMethod method : candidate.getDiscoveryMethods().stream()
                    .filter(method -> SERVICE_TYPE.equals(method.getServiceType())).toList()) {
                Map<String, Pattern> matchProperties = method.getMatchProperties().stream()
                        .collect(Collectors.toMap(property -> property.getName(), property -> property.getPattern()));

                Set<String> matchPropertyKeys = matchProperties.keySet().stream()
                        .filter(property -> (!NAME.equals(property) && !APPLICATION.equals(property)))
                        .collect(Collectors.toSet());

                logger.trace("Checking candidate: {}", candidate.getUID());
                for (ServiceInfo service : services.values()) {

                    logger.trace("Checking service: {}/{}", service.getQualifiedName(), service.getNiceTextString());
                    if (method.getMdnsServiceType().equals(service.getType())
                            && propertyMatches(matchProperties, NAME, service.getName())
                            && propertyMatches(matchProperties, APPLICATION, service.getApplication())
                            && matchPropertyKeys.stream().allMatch(
                                    name -> propertyMatches(matchProperties, name, service.getPropertyString(name)))) {
                        result.add(candidate);
                        logger.debug("Suggested addon found: {}", candidate.getUID());
                        break;
                    }
                }
            }
        }
        return result;
    }

    @Override
    public void serviceAdded(@Nullable ServiceEvent event) {
        if (event != null) {
            ServiceInfo service = event.getInfo();
            if (service != null) {
                addService(service, false);
            }
        }
    }

    @Override
    public void serviceRemoved(@Nullable ServiceEvent event) {
    }

    @Override
    public void serviceResolved(@Nullable ServiceEvent event) {
        if (event != null) {
            ServiceInfo service = event.getInfo();
            if (service != null) {
                addService(service, true);
            }
        }
    }

    @Override
    protected void resetAddonCandidates() {
        addonCandidates.forEach(c -> c.getDiscoveryMethods().stream()
                .filter(m -> SERVICE_TYPE.equals(m.getServiceType())).filter(m -> !m.getMdnsServiceType().isEmpty())
                .forEach(m -> mdnsClient.removeServiceListener(m.getMdnsServiceType(), this)));
        super.resetAddonCandidates();
    }

    @Override
    public void setAddonCandidates(List<AddonInfo> candidates) {
        super.setAddonCandidates(candidates);
        candidates.forEach(c -> c.getDiscoveryMethods().stream().filter(m -> SERVICE_TYPE.equals(m.getServiceType()))
                .filter(m -> !m.getMdnsServiceType().isEmpty()).forEach(m -> {
                    String serviceType = m.getMdnsServiceType();
                    mdnsClient.addServiceListener(serviceType, this);
                    scheduler.submit(() -> mdnsClient.list(serviceType));
                }));
    }
}
