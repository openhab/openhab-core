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
package org.openhab.core.config.discovery.addon.mdns;

import static org.openhab.core.config.discovery.addon.AddonFinderConstants.*;

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
import org.openhab.core.config.discovery.addon.AddonFinder;
import org.openhab.core.config.discovery.addon.BaseAddonFinder;
import org.openhab.core.io.transport.mdns.MDNSClient;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a {@link MDNSAddonFinder} for finding suggested add-ons via mDNS. This finder requires a
 * {@code mdnsServiceType} parameter to be present in the add-on info discovery method.
 *
 * @author Andrew Fiddian-Green - Initial contribution
 * @author Mark Herwege - refactor to allow uninstall
 * @author Mark Herwege - change to discovery method schema
 */
@NonNullByDefault
@Component(service = AddonFinder.class, name = MDNSAddonFinder.SERVICE_NAME)
public class MDNSAddonFinder extends BaseAddonFinder implements ServiceListener {

    public static final String SERVICE_TYPE = SERVICE_TYPE_MDNS;
    public static final String SERVICE_NAME = SERVICE_NAME_MDNS;

    public static final String MDNS_SERVICE_TYPE = "mdnsServiceType";

    private static final String NAME = "name";
    private static final String APPLICATION = "application";

    private final Logger logger = LoggerFactory.getLogger(MDNSAddonFinder.class);
    private final ScheduledExecutorService scheduler = ThreadPoolManager.getScheduledPool(SERVICE_NAME);
    private final Map<String, ServiceInfo> services = new ConcurrentHashMap<>();
    private MDNSClient mdnsClient;

    @Activate
    public MDNSAddonFinder(@Reference MDNSClient mdnsClient) {
        this.mdnsClient = mdnsClient;
    }

    /**
     * Adds the given mDNS service to the set of discovered services.
     *
     * @param service the mDNS service to be added.
     * @param isResolved indicates if mDNS has fully resolved the service information.
     */
    public void addService(ServiceInfo service, boolean isResolved) {
        String qualifiedName = service.getQualifiedName();
        if (isResolved || !services.containsKey(qualifiedName)) {
            if (services.put(qualifiedName, service) == null) {
                logger.trace("Added service: {}", qualifiedName);
            }
        }
    }

    @Deactivate
    public void deactivate() {
        services.clear();
        unsetAddonCandidates();
    }

    @Override
    public void setAddonCandidates(List<AddonInfo> candidates) {
        // Remove listeners for all service types that are no longer in candidates
        addonCandidates.stream().filter(c -> !candidates.contains(c))
                .forEach(c -> c.getDiscoveryMethods().stream().filter(m -> SERVICE_TYPE.equals(m.getServiceType()))
                        .filter(m -> !getMdnsServiceType(m).isEmpty())
                        .forEach(m -> mdnsClient.removeServiceListener(getMdnsServiceType(m), this)));

        // Add listeners for all service types in candidates
        super.setAddonCandidates(candidates);
        addonCandidates
                .forEach(c -> c.getDiscoveryMethods().stream().filter(m -> SERVICE_TYPE.equals(m.getServiceType()))
                        .filter(m -> !getMdnsServiceType(m).isEmpty()).forEach(m -> {
                            String serviceType = getMdnsServiceType(m);
                            mdnsClient.addServiceListener(serviceType, this);
                            scheduler.submit(() -> mdnsClient.list(serviceType));
                        }));
    }

    @Override
    public void unsetAddonCandidates() {
        addonCandidates.forEach(c -> c.getDiscoveryMethods().stream()
                .filter(m -> SERVICE_TYPE.equals(m.getServiceType())).filter(m -> !getMdnsServiceType(m).isEmpty())
                .forEach(m -> mdnsClient.removeServiceListener(getMdnsServiceType(m), this)));
        super.unsetAddonCandidates();
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
                    if (getMdnsServiceType(method).equals(service.getType())
                            && propertyMatches(matchProperties, NAME, service.getName())
                            && propertyMatches(matchProperties, APPLICATION, service.getApplication())
                            && matchPropertyKeys.stream().allMatch(
                                    name -> propertyMatches(matchProperties, name, service.getPropertyString(name)))) {
                        result.add(candidate);
                        logger.debug("Suggested add-on found: {}", candidate.getUID());
                        break;
                    }
                }
            }
        }
        return result;
    }

    private String getMdnsServiceType(AddonDiscoveryMethod method) {
        String param = method.getParameters().stream().filter(p -> MDNS_SERVICE_TYPE.equals(p.getName()))
                .map(p -> p.getValue()).findFirst().orElse("");
        return param == null ? "" : param;
    }

    @Override
    public String getServiceName() {
        return SERVICE_NAME;
    }

    /*
     * ************ MDNSClient call-back methods ************
     */

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
}
