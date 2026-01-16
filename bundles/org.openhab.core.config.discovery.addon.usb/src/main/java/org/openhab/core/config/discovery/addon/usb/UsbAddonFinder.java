/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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

import java.util.HashMap;
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
import org.openhab.core.config.discovery.usbserial.UsbSerialDeviceInformation;
import org.openhab.core.config.discovery.usbserial.UsbSerialDiscovery;
import org.openhab.core.config.discovery.usbserial.UsbSerialDiscoveryListener;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a {@link AddonFinder} for finding suggested add-ons related to USB devices.
 * <p>
 * It supports the following values for the 'match-property' 'name' element:
 * <li>product - match on the product description text
 * <li>manufacturer - match on the device manufacturer text
 * <li>chipId - match on the chip vendor id plus product id
 * <li>remote - match on whether the device is connected remotely or locally
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
@NonNullByDefault
@Component(service = AddonFinder.class, name = UsbAddonFinder.SERVICE_NAME)
public class UsbAddonFinder extends BaseAddonFinder implements UsbSerialDiscoveryListener {

    public static final String SERVICE_TYPE = SERVICE_TYPE_USB;
    public static final String SERVICE_NAME = SERVICE_NAME_USB;

    /*
     * Supported 'match-property' names
     */
    public static final String PRODUCT = "product";
    public static final String MANUFACTURER = "manufacturer";
    public static final String CHIP_ID = "chipId";
    public static final String REMOTE = "remote";

    public static final Set<String> SUPPORTED_PROPERTIES = Set.of(PRODUCT, MANUFACTURER, CHIP_ID, REMOTE);

    private final Logger logger = LoggerFactory.getLogger(UsbAddonFinder.class);

    // All access must be guarded by "this"
    private final Map<Long, UsbSerialDeviceInformation> usbDeviceInformations = new HashMap<>();

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addUsbSerialDiscovery(UsbSerialDiscovery usbSerialDiscovery) {
        usbSerialDiscovery.registerDiscoveryListener(this);
    }

    protected synchronized void removeUsbSerialDiscovery(UsbSerialDiscovery usbSerialDiscovery) {
        usbSerialDiscovery.unregisterDiscoveryListener(this);
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
                synchronized (this) {
                    for (UsbSerialDeviceInformation device : usbDeviceInformations.values()) {
                        logger.trace("Checking device: {}", device);

                        if (propertyMatches(matchProperties, PRODUCT, device.getProduct())
                                && propertyMatches(matchProperties, MANUFACTURER, device.getManufacturer())
                                && propertyMatches(matchProperties, CHIP_ID,
                                        getChipId(device.getVendorId(), device.getProductId()))
                                && propertyMatches(matchProperties, REMOTE, String.valueOf(device.getRemote()))) {
                            result.add(candidate);
                            logger.debug("Suggested add-on found: {}", candidate.getUID());
                            break;
                        }
                    }
                }
            }
        }
        return result;
    }

    private String getChipId(int vendorId, int productId) {
        return String.format("%04x:%04x", vendorId, productId);
    }

    @Override
    public String getServiceName() {
        return SERVICE_NAME;
    }

    /**
     * Create a unique 33 bit integer map hash key comprising the remote flag in the upper bit, the vendorId in the
     * middle 16 bits, and the productId in the lower 16 bits.
     */
    private long keyOf(UsbSerialDeviceInformation deviceInfo) {
        return (deviceInfo.getRemote() ? 0x1_0000_0000L : 0) + (deviceInfo.getVendorId() * 0x1_0000L)
                + deviceInfo.getProductId();
    }

    /**
     * Add the discovered USB device information record to our internal map. If there is already an entry in the map
     * then merge the two sets of data.
     *
     * @param discoveredInfo the newly discovered USB device information.
     */
    @Override
    public void usbSerialDeviceDiscovered(UsbSerialDeviceInformation discoveredInfo) {
        UsbSerialDeviceInformation targetInfo = discoveredInfo;
        synchronized (this) {
            UsbSerialDeviceInformation existingInfo = usbDeviceInformations.get(keyOf(targetInfo));

            if (existingInfo != null) {
                boolean isMerging = false;
                String product = existingInfo.getProduct();
                if (product != null) {
                    product = discoveredInfo.getProduct();
                    isMerging = true;
                }
                String manufacturer = existingInfo.getManufacturer();
                if (manufacturer != null) {
                    manufacturer = discoveredInfo.getManufacturer();
                    isMerging = true;
                }
                String serialNumber = existingInfo.getSerialNumber();
                if (serialNumber != null) {
                    serialNumber = discoveredInfo.getSerialNumber();
                    isMerging = true;
                }
                boolean remote = existingInfo.getRemote();
                if (remote == discoveredInfo.getRemote()) {
                    isMerging = true;
                }
                if (isMerging) {
                    targetInfo = new UsbSerialDeviceInformation(discoveredInfo.getVendorId(),
                            discoveredInfo.getProductId(), serialNumber, manufacturer, product,
                            discoveredInfo.getInterfaceNumber(), discoveredInfo.getInterfaceDescription(),
                            discoveredInfo.getSerialPort()).setRemote(remote);
                }
            }

            usbDeviceInformations.put(keyOf(targetInfo), targetInfo);
        }
    }

    @Override
    public synchronized void usbSerialDeviceRemoved(UsbSerialDeviceInformation removedInfo) {
        usbDeviceInformations.remove(keyOf(removedInfo));
    }
}
