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
package org.openhab.core.config.discovery.addon.upnp;

import static org.openhab.core.config.discovery.addon.AddonFinderConstants.SERVICE_NAME_UPNP;
import static org.openhab.core.config.discovery.addon.AddonFinderConstants.SERVICE_TYPE_UPNP;

import java.net.URI;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.jupnp.UpnpService;
import org.jupnp.model.message.header.RootDeviceHeader;
import org.jupnp.model.meta.DeviceDetails;
import org.jupnp.model.meta.LocalDevice;
import org.jupnp.model.meta.ManufacturerDetails;
import org.jupnp.model.meta.ModelDetails;
import org.jupnp.model.meta.RemoteDevice;
import org.jupnp.model.meta.RemoteDeviceIdentity;
import org.jupnp.model.types.DeviceType;
import org.jupnp.model.types.UDN;
import org.jupnp.registry.Registry;
import org.jupnp.registry.RegistryListener;
import org.openhab.core.addon.AddonDiscoveryMethod;
import org.openhab.core.addon.AddonInfo;
import org.openhab.core.addon.AddonMatchProperty;
import org.openhab.core.config.discovery.addon.AddonFinder;
import org.openhab.core.config.discovery.addon.BaseAddonFinder;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a {@link UpnpAddonFinder} for finding suggested Addons via UPnP.
 *
 * @author Andrew Fiddian-Green - Initial contribution
 * @author Mark Herwege - refactor to allow uninstall
 */
@NonNullByDefault
@Component(service = AddonFinder.class, name = UpnpAddonFinder.SERVICE_NAME)
public class UpnpAddonFinder extends BaseAddonFinder implements RegistryListener {

    public static final String SERVICE_TYPE = SERVICE_TYPE_UPNP;
    public static final String SERVICE_NAME = SERVICE_NAME_UPNP;

    private static final String DEVICE_TYPE = "deviceType";
    private static final String MANUFACTURER = "manufacturer";
    private static final String MANUFACTURER_URI = "manufacturerURI";
    private static final String MODEL_NAME = "modelName";
    private static final String MODEL_NUMBER = "modelNumber";
    private static final String MODEL_DESCRIPTION = "modelDescription";
    private static final String MODEL_URI = "modelURI";
    private static final String SERIAL_NUMBER = "serialNumber";
    private static final String FRIENDLY_NAME = "friendlyName";

    private static final Set<String> SUPPORTED_PROPERTIES = Set.of(DEVICE_TYPE, MANUFACTURER, MANUFACTURER_URI,
            MODEL_NAME, MODEL_NUMBER, MODEL_DESCRIPTION, MODEL_URI, SERIAL_NUMBER, FRIENDLY_NAME);

    private final Logger logger = LoggerFactory.getLogger(UpnpAddonFinder.class);
    private final Map<String, RemoteDevice> devices = new ConcurrentHashMap<>();
    private final UpnpService upnpService;

    @Activate
    public UpnpAddonFinder(@Reference UpnpService upnpService) {
        this.upnpService = upnpService;

        Registry registry = upnpService.getRegistry();
        for (RemoteDevice device : registry.getRemoteDevices()) {
            remoteDeviceAdded(registry, device);
        }
        registry.addListener(this);
        upnpService.getControlPoint().search();
        upnpService.getControlPoint().search(new RootDeviceHeader());
    }

    @Deactivate
    public void deactivate() {
        unsetAddonCandidates();

        UpnpService upnpService = this.upnpService;
        upnpService.getRegistry().removeListener(this);

        devices.clear();
    }

    /**
     * Adds the given UPnP remote device to the set of discovered devices.
     *
     * @param device the UPnP remote device to be added.
     */
    private void addDevice(RemoteDevice device) {
        RemoteDeviceIdentity identity = device.getIdentity();
        if (identity != null) {
            UDN udn = identity.getUdn();
            if (udn != null) {
                String udnString = udn.getIdentifierString();
                if (devices.put(udnString, device) == null) {
                    logger.trace("Added device: {}", device.getDisplayString());
                }
            }
        }
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
                for (RemoteDevice device : devices.values()) {

                    String deviceType = null;
                    String serialNumber = null;
                    String friendlyName = null;
                    String manufacturer = null;
                    String manufacturerURI = null;
                    String modelName = null;
                    String modelNumber = null;
                    String modelDescription = null;
                    String modelURI = null;

                    DeviceType devType = device.getType();
                    if (devType != null) {
                        deviceType = devType.getType();
                    }

                    DeviceDetails devDetails = device.getDetails();
                    if (devDetails != null) {
                        friendlyName = devDetails.getFriendlyName();
                        serialNumber = devDetails.getSerialNumber();

                        ManufacturerDetails mfrDetails = devDetails.getManufacturerDetails();
                        if (mfrDetails != null) {
                            URI mfrUri = mfrDetails.getManufacturerURI();
                            manufacturer = mfrDetails.getManufacturer();
                            manufacturerURI = mfrUri != null ? mfrUri.toString() : null;
                        }

                        ModelDetails modDetails = devDetails.getModelDetails();
                        if (modDetails != null) {
                            URI modUri = modDetails.getModelURI();
                            modelName = modDetails.getModelName();
                            modelDescription = modDetails.getModelDescription();
                            modelNumber = modDetails.getModelNumber();
                            modelURI = modUri != null ? modUri.toString() : null;
                        }
                    }

                    logger.trace("Checking device: {}", device.getDisplayString());
                    if (propertyMatches(matchProperties, DEVICE_TYPE, deviceType)
                            && propertyMatches(matchProperties, MANUFACTURER, manufacturer)
                            && propertyMatches(matchProperties, MANUFACTURER_URI, manufacturerURI)
                            && propertyMatches(matchProperties, MODEL_NAME, modelName)
                            && propertyMatches(matchProperties, MODEL_NUMBER, modelNumber)
                            && propertyMatches(matchProperties, MODEL_DESCRIPTION, modelDescription)
                            && propertyMatches(matchProperties, MODEL_URI, modelURI)
                            && propertyMatches(matchProperties, SERIAL_NUMBER, serialNumber)
                            && propertyMatches(matchProperties, FRIENDLY_NAME, friendlyName)) {
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
    public String getServiceName() {
        return SERVICE_NAME;
    }

    /*
     * ************ UpnpService call-back methods ************
     */

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
        if (remoteDevice != null) {
            addDevice(remoteDevice);
        }
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
        if (remoteDevice != null) {
            addDevice(remoteDevice);
        }
    }
}
