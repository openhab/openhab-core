/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
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

import static org.openhab.core.config.discovery.addon.AddonFinderConstants.SERVICE_NAME_SDDP;
import static org.openhab.core.config.discovery.addon.AddonFinderConstants.SERVICE_TYPE_SDDP;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.addon.AddonDiscoveryMethod;
import org.openhab.core.addon.AddonInfo;
import org.openhab.core.addon.AddonMatchProperty;
import org.openhab.core.config.discovery.addon.AddonFinder;
import org.openhab.core.config.discovery.addon.BaseAddonFinder;
import org.openhab.core.config.discovery.sddp.SddpDevice;
import org.openhab.core.config.discovery.sddp.SddpDeviceParticipant;
import org.openhab.core.config.discovery.sddp.SddpDiscoveryServiceInterface;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a {@link SddpAddonFinder} for finding suggested Addons via SDDP.
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
@Component(immediate = true, service = AddonFinder.class, name = SddpAddonFinder.SERVICE_NAME)
public class SddpAddonFinder extends BaseAddonFinder implements SddpDeviceParticipant {

    public static final String SERVICE_TYPE = SERVICE_TYPE_SDDP;
    public static final String SERVICE_NAME = SERVICE_NAME_SDDP;

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

    @SuppressWarnings("unused") // keep the {@link SddpDiscoveryService} loaded
    private final SddpDiscoveryServiceInterface sddpDiscoveryService;

    @Activate
    public SddpAddonFinder(@Reference SddpDiscoveryServiceInterface sddpDiscoveryService) {
        logger.trace("SddpAddonFinder()");
        this.sddpDiscoveryService = sddpDiscoveryService;
    }

    @Deactivate
    public void deactivate() {
        logger.trace("deactivate()");
        unsetAddonCandidates();
        foundDevices.clear();
    }

    @Override
    public void deviceAdded(SddpDevice device) {
        logger.trace("deviceAdded()");
        foundDevices.add(device);
    }

    @Override
    public void deviceRemoved(SddpDevice device) {
        logger.trace("deviceRemoved()");
        foundDevices.remove(device);
    }

    @Override
    public void setAddonCandidates(List<AddonInfo> candidates) {
        super.setAddonCandidates(candidates);
    }

    @Override
    public String getServiceName() {
        logger.trace("getServiceName() {}", SERVICE_NAME);
        return SERVICE_NAME;
    }

    @Override
    public Set<AddonInfo> getSuggestedAddons() {
        logger.trace("getSuggestedAddons()");
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
}
