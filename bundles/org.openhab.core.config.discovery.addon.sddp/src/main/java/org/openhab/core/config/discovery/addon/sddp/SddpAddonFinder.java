/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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
package org.openhab.core.config.discovery.addon.sddp;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.addon.AddonDiscoveryMethod;
import org.openhab.core.addon.AddonInfo;
import org.openhab.core.addon.AddonMatchProperty;
import org.openhab.core.config.discovery.DiscoveryService;
import org.openhab.core.config.discovery.addon.AddonFinder;
import org.openhab.core.config.discovery.addon.AddonFinderConstants;
import org.openhab.core.config.discovery.addon.BaseAddonFinder;
import org.openhab.core.config.discovery.sddp.SddpDevice;
import org.openhab.core.config.discovery.sddp.SddpDeviceParticipant;
import org.openhab.core.config.discovery.sddp.SddpDiscoveryService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a {@link SddpAddonFinder} for finding suggested Addons via SDDP.
 * <p>
 * Simple Device Discovery Protocol (SDDP) is a simple multicast discovery protocol implemented
 * by many "smart home" devices to allow a controlling agent to easily discover and connect to
 * devices on a local subnet.
 * <p>
 * SDDP was created by Control4, and is quite similar to UPnP's standard Simple Service Discovery
 * Protocol (SSDP), and it serves a virtually identical purpose. SDDP is not a standard protocol
 * and it is not publicly documented.
 * <p>
 * It checks the binding's addon.xml 'match-property' elements for the following SDDP properties:
 * <li>driver</li>
 * <li>host</li>
 * <li>ipAddress</li>
 * <li>macAddress</li>
 * <li>manufacturer</li>
 * <li>model</li>
 * <li>port</li>
 * <li>primaryProxy</li>
 * <li>proxies</li>
 * <li>type</li>
 * <p>
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
@NonNullByDefault
@Component(service = AddonFinder.class, name = SddpAddonFinder.SERVICE_NAME)
public class SddpAddonFinder extends BaseAddonFinder implements SddpDeviceParticipant {

    public static final String SERVICE_TYPE = AddonFinderConstants.SERVICE_TYPE_SDDP;
    public static final String SERVICE_NAME = AddonFinderConstants.SERVICE_NAME_SDDP;

    private static final String DRIVER = "driver";
    private static final String HOST = "host";
    private static final String IP_ADDRESS = "ipAddress";
    private static final String MAC_ADDRESS = "macAddress";
    private static final String MANUFACTURER = "manufacturer";
    private static final String MODEL = "model";
    private static final String PORT = "port";
    private static final String PRIMARY_PROXY = "primaryProxy";
    private static final String PROXIES = "proxies";
    private static final String TYPE = "type";

    private static final Set<String> SUPPORTED_PROPERTIES = Set.of(DRIVER, HOST, IP_ADDRESS, MAC_ADDRESS, MANUFACTURER,
            MODEL, PORT, PRIMARY_PROXY, PROXIES, TYPE);

    private final Logger logger = LoggerFactory.getLogger(SddpAddonFinder.class);
    private final Set<SddpDevice> foundDevices = new HashSet<>();

    private @Nullable SddpDiscoveryService sddpDiscoveryService = null;

    @Activate
    public SddpAddonFinder(
            @Reference(service = DiscoveryService.class, target = "(protocol=sddp)") DiscoveryService discoveryService) {
        if (discoveryService instanceof SddpDiscoveryService sddpDiscoveryService) {
            sddpDiscoveryService.addSddpDeviceParticipant(this);
            this.sddpDiscoveryService = sddpDiscoveryService;
        } else {
            logger.warn("SddpAddonFinder() DiscoveryService is not an SddpDiscoveryService");
        }
    }

    @Deactivate
    public void deactivate() {
        SddpDiscoveryService sddpDiscoveryService = this.sddpDiscoveryService;
        if (sddpDiscoveryService != null) {
            sddpDiscoveryService.removeSddpDeviceParticipant(this);
            this.sddpDiscoveryService = null;
        }
        unsetAddonCandidates();
        foundDevices.clear();
    }

    @Override
    public void deviceAdded(SddpDevice device) {
        foundDevices.add(device);
    }

    @Override
    public void deviceRemoved(SddpDevice device) {
        foundDevices.remove(device);
    }

    @Override
    public String getServiceName() {
        return SERVICE_NAME;
    }

    @Override
    public Set<AddonInfo> getSuggestedAddons() {
        Set<AddonInfo> result = new HashSet<>();
        for (AddonInfo candidate : addonCandidates) {
            for (AddonDiscoveryMethod method : candidate.getDiscoveryMethods().stream()
                    .filter(method -> SERVICE_TYPE.equals(method.getServiceType())).toList()) {
                Map<String, Pattern> matchProperties = method.getMatchProperties().stream()
                        .collect(Collectors.toMap(AddonMatchProperty::getName, AddonMatchProperty::getPattern));

                Set<String> propertyNames = new HashSet<>(matchProperties.keySet());
                propertyNames.removeAll(SUPPORTED_PROPERTIES);

                if (!propertyNames.isEmpty()) {
                    logger.warn("Add-on '{}' addon.xml file contains unsupported 'match-property' [{}]",
                            candidate.getUID(), String.join(",", propertyNames));
                    break;
                }

                logger.trace("Checking candidate: {}", candidate.getUID());
                for (SddpDevice device : foundDevices) {
                    logger.trace("Checking device: {}", device.host);
                    if (propertyMatches(matchProperties, HOST, device.host)
                            && propertyMatches(matchProperties, IP_ADDRESS, device.ipAddress)
                            && propertyMatches(matchProperties, MAC_ADDRESS, device.macAddress)
                            && propertyMatches(matchProperties, MANUFACTURER, device.manufacturer)
                            && propertyMatches(matchProperties, MODEL, device.model)
                            && propertyMatches(matchProperties, PORT, device.port)
                            && propertyMatches(matchProperties, PRIMARY_PROXY, device.primaryProxy)
                            && propertyMatches(matchProperties, PROXIES, device.proxies)
                            && propertyMatches(matchProperties, TYPE, device.type)) {
                        result.add(candidate);
                        logger.debug("Suggested add-on found: {}", candidate.getUID());
                        break;
                    }
                }
            }
        }
        return result;
    }

    @Override
    public void setAddonCandidates(List<AddonInfo> candidates) {
        super.setAddonCandidates(candidates);
    }
}
