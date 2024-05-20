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
import org.openhab.core.config.discovery.sddp.SddpDeviceListener;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a {@link SddpAddonFinder} for finding suggested Addons via SDDP.
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
@NonNullByDefault
@Component(service = AddonFinder.class, name = SddpAddonFinder.SERVICE_NAME)
public class SddpAddonFinder extends BaseAddonFinder implements SddpDeviceListener {

    public static final String SERVICE_TYPE = SERVICE_TYPE_SDDP;
    public static final String SERVICE_NAME = SERVICE_NAME_SDDP;

    private static final String DRIVER = "driver";
    private static final String HOST = "host";
    private static final String MANUFACTURER = "manufacturer";
    private static final String MODEL = "model";
    private static final String PRIMARY_PROXY = "primaryProxy";
    private static final String PROXIES = "proxies";
    private static final String TYPE = "type";

    private static final Set<String> SUPPORTED_PROPERTIES = //
            Set.of(DRIVER, HOST, MANUFACTURER, MODEL, PRIMARY_PROXY, PROXIES, TYPE);

    private final Logger logger = LoggerFactory.getLogger(SddpAddonFinder.class);
    private final Set<SddpDevice> devices = new HashSet<>();

    @Activate
    public SddpAddonFinder() {
    }

    @Deactivate
    public void deactivate() {
        unsetAddonCandidates();
        devices.clear();
    }

    @Override
    public void deviceAdded(SddpDevice device) {
        devices.add(device);
    }

    @Override
    public void deviceRemoved(SddpDevice device) {
        devices.remove(device);
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
                for (SddpDevice device : devices) {
                    logger.trace("Checking device: {}", device.host);
                    if (propertyMatches(matchProperties, HOST, device.host)
                            && propertyMatches(matchProperties, MANUFACTURER, device.manufacturer)
                            && propertyMatches(matchProperties, MODEL, device.model)
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
