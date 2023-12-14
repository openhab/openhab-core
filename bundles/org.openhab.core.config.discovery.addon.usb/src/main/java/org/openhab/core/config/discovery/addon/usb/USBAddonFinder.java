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
package org.openhab.core.config.discovery.addon.usb;

import static org.openhab.core.config.discovery.addon.AddonFinderConstants.SERVICE_NAME_USB;
import static org.openhab.core.config.discovery.addon.AddonFinderConstants.SERVICE_TYPE_USB;

import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.usb.UsbDevice;
import javax.usb.UsbDeviceDescriptor;
import javax.usb.UsbDisconnectedException;
import javax.usb.UsbException;
import javax.usb.UsbHostManager;
import javax.usb.UsbHub;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.addon.AddonDiscoveryMethod;
import org.openhab.core.addon.AddonInfo;
import org.openhab.core.config.discovery.addon.AddonFinder;
import org.openhab.core.config.discovery.addon.BaseAddonFinder;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a {@link USBAddonFinder} for finding suggested add-ons related to USB devices.
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
@NonNullByDefault
@Component(service = AddonFinder.class, name = USBAddonFinder.SERVICE_NAME)
public class USBAddonFinder extends BaseAddonFinder {

    public static final String SERVICE_TYPE = SERVICE_TYPE_USB;
    public static final String SERVICE_NAME = SERVICE_NAME_USB;

    private static final String VENDOR_ID = "vendorId";
    private static final String VENDOR_NAME = "vendorName";
    private static final String PRODUCT_ID = "productId";
    private static final String PRODUCT_NAME = "productName";

    private static final Set<String> SUPPORTED_PROPERTIES = Set.of(VENDOR_ID, VENDOR_NAME, PRODUCT_ID, PRODUCT_NAME);

    /**
     * Private class for storing the properties of discovered USB devices.
     */
    protected static class USBDevice {
        private final String vendorId;
        private final String productId;
        private @Nullable String vendorName;
        private @Nullable String productName;

        public USBDevice(UsbDevice usbDevice) {
            UsbDeviceDescriptor descriptor = usbDevice.getUsbDeviceDescriptor();
            vendorId = String.format("%04x", descriptor.idVendor());
            productId = String.format("%04x", descriptor.idProduct());
            try {
                vendorName = usbDevice.getManufacturerString();
            } catch (UnsupportedEncodingException | UsbDisconnectedException | UsbException e) {
                vendorName = null;
            }
            try {
                productName = usbDevice.getProductString();
            } catch (UnsupportedEncodingException | UsbDisconnectedException | UsbException e) {
                productName = null;
            }
        }

        @Override
        public String toString() {
            return String.format("vendorId:%s (vendorName:%s) / productId:%s (productName:%s)", vendorId, vendorName,
                    productId, productName);
        }
    }

    private final Logger logger = LoggerFactory.getLogger(USBAddonFinder.class);
    private final List<USBDevice> deviceData = new CopyOnWriteArrayList<>();

    @Activate
    public USBAddonFinder() {
    }

    @Deactivate
    public void deactivate() {
    }

    /**
     * Traverse the USB tree for devices that are children of the given hub.
     * 
     * @param usbHub the hub whose children are to be found.
     */
    private void usbGetChildren(UsbHub usbHub) {
        @SuppressWarnings("unchecked")
        List<UsbDevice> deviceList = usbHub.getAttachedUsbDevices();
        deviceList.forEach(usbDevice -> {
            if (usbDevice.isUsbHub()) {
                usbGetChildren((UsbHub) usbDevice);
            } else {
                USBDevice data = new USBDevice(usbDevice);
                deviceData.add(data);
                logger.trace("Added device: {}", data);
            }
        });
    }

    /**
     * Traverse the USB tree for devices that are children of the root hub.
     */
    private void usbGetDevices() {
        try {
            usbGetChildren(UsbHostManager.getUsbServices().getRootUsbHub());
            logger.trace("USB devices found: {}", deviceData.size());
        } catch (SecurityException | UsbException e) {
            logger.warn("Error getting USB devices: {}", e.getMessage());
        }
    }

    @Override
    public void setAddonCandidates(List<AddonInfo> candidates) {
        super.setAddonCandidates(candidates);
    }

    @Override
    public void unsetAddonCandidates() {
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

                Set<String> propertyNames = new HashSet<>(matchProperties.keySet());
                propertyNames.removeAll(SUPPORTED_PROPERTIES);

                if (!propertyNames.isEmpty()) {
                    logger.warn("Add-on '{}' addon.xml file contains unsupported 'match-property' [{}]",
                            candidate.getUID(), String.join(",", propertyNames));
                    break;
                }

                logger.trace("Checking candidate: {}", candidate.getUID());
                for (USBDevice device : getDeviceData()) {
                    logger.trace("Checking device: {}", device);
                    if (propertyMatches(matchProperties, VENDOR_ID, device.vendorId)
                            && propertyMatches(matchProperties, VENDOR_NAME, device.vendorName)
                            && propertyMatches(matchProperties, PRODUCT_ID, device.productId)
                            && propertyMatches(matchProperties, PRODUCT_NAME, device.productName)) {
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

    public List<USBDevice> getDeviceData() {
        if (deviceData.isEmpty()) {
            usbGetDevices();
        }
        return deviceData;
    }
}
