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
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.jupnp.UpnpService;
import org.jupnp.model.meta.DeviceDetails;
import org.jupnp.model.meta.LocalDevice;
import org.jupnp.model.meta.ManufacturerDetails;
import org.jupnp.model.meta.ModelDetails;
import org.jupnp.model.meta.RemoteDevice;
import org.jupnp.registry.Registry;
import org.jupnp.registry.RegistryListener;
import org.openhab.core.addon.AddonInfo;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a {@link UpnpAddonSuggestionFinder} for finding suggested Addons via UPnP.
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
@NonNullByDefault
@Component(service = AddonSuggestionFinder.class, name = UpnpAddonSuggestionFinder.SERVICE_NAME)
public class UpnpAddonSuggestionFinder extends BaseAddonSuggestionFinder implements RegistryListener {

    public static final String SERVICE_TYPE = "upnp";
    public static final String SERVICE_NAME = SERVICE_TYPE + ADDON_SUGGESTION_FINDER;

    private static final String DEVICE_TYPE = "deviceType";
    private static final String MANUFACTURER = "manufacturer";
    private static final String MANUFACTURER_URI = "manufacturerURI";
    private static final String MODEL_NAME = "modelName";
    private static final String MODEL_NUMBER = "modelNumber";
    private static final String SERIAL_NUMBER = "serialNumber";
    private static final String FRIENDLY_NAME = "friendlyName";
    private static final String UDN = "udn";

    private static final Set<String> SUPPORTED_PROPERTIES = Set.of(DEVICE_TYPE, MANUFACTURER, MANUFACTURER_URI,
            MODEL_NAME, MODEL_NUMBER, SERIAL_NUMBER, FRIENDLY_NAME, UDN);

    private final Logger logger = LoggerFactory.getLogger(UpnpAddonSuggestionFinder.class);
    private final Set<RemoteDevice> devices = ConcurrentHashMap.newKeySet();
    private final UpnpService upnpService;

    @Activate
    public UpnpAddonSuggestionFinder(@Reference UpnpService upnpService) {
        this.upnpService = upnpService;
        this.upnpService.getRegistry().addListener(this);
    }

    public void addDevice(@Nullable RemoteDevice device) {
        if (device != null) {
            devices.add(device);
        }
    }

    @Deactivate
    public void close() {
        devices.clear();
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

                        Set<String> propNames = new HashSet<>(map.keySet());
                        propNames.removeAll(SUPPORTED_PROPERTIES);
                        if (!propNames.isEmpty()) {
                            logger.warn("Addon '{}' addon.xml file contains unsupported 'match-property' [{}]",
                                    candidate.getUID(), String.join(",", propNames));
                        } else {
                            devices.stream().forEach(device -> {
                                DeviceDetails deviceDetails = device.getDetails();
                                ManufacturerDetails manufacturerDetails = deviceDetails.getManufacturerDetails();
                                ModelDetails modelDetails = deviceDetails.getModelDetails();
                                if (propertyMatches(map, DEVICE_TYPE, device.getType().getType())
                                        && propertyMatches(map, MANUFACTURER, manufacturerDetails.getManufacturer())
                                        && propertyMatches(map, MANUFACTURER_URI,
                                                manufacturerDetails.getManufacturerURI().toString())
                                        && propertyMatches(map, MODEL_NAME, modelDetails.getModelName())
                                        && propertyMatches(map, MODEL_NUMBER, modelDetails.getModelNumber())
                                        && propertyMatches(map, SERIAL_NUMBER, deviceDetails.getSerialNumber())
                                        && propertyMatches(map, FRIENDLY_NAME, deviceDetails.getFriendlyName())
                                        && propertyMatches(map, UDN,
                                                device.getIdentity().getUdn().getIdentifierString())) {
                                    result.add(candidate);
                                    logger.debug("Addon '{}' will be suggested", candidate.getUID());
                                }
                            });
                        }
                    });
        });
        return result;
    }

    @Override
    public void afterShutdown() {
    }

    @Override
    public void beforeShutdown(@Nullable Registry registry) {
    }

    @Override
    public void localDeviceAdded(@Nullable Registry registry, @Nullable LocalDevice localDevice) {
    }

    @Override
    public void localDeviceRemoved(@Nullable Registry registry, @Nullable LocalDevice localDevice) {
    }

    @Override
    public void remoteDeviceAdded(@Nullable Registry registry, @Nullable RemoteDevice remoteDevice) {
        addDevice(remoteDevice);
    }

    @Override
    public void remoteDeviceDiscoveryFailed(@Nullable Registry registry, @Nullable RemoteDevice remoteDevice,
            @Nullable Exception exception) {
    }

    @Override
    public void remoteDeviceDiscoveryStarted(@Nullable Registry registry, @Nullable RemoteDevice remoteDevice) {
    }

    @Override
    public void remoteDeviceRemoved(@Nullable Registry registry, @Nullable RemoteDevice remoteDevice) {
    }

    @Override
    public void remoteDeviceUpdated(@Nullable Registry registry, @Nullable RemoteDevice remoteDevice) {
        addDevice(remoteDevice);
    }
}
