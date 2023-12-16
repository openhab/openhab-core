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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.addon.AddonDiscoveryMethod;
import org.openhab.core.addon.AddonInfo;
import org.openhab.core.config.discovery.addon.AddonFinder;
import org.openhab.core.config.discovery.addon.BaseAddonFinder;
import org.openhab.core.config.discovery.usbserial.UsbSerialDeviceInformation;
import org.openhab.core.config.discovery.usbserial.UsbSerialDiscovery;
import org.openhab.core.config.discovery.usbserial.UsbSerialDiscoveryListener;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is an implementation of the {@link AddonFinder} interface that finds suggested add-ons related to USB devices.
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
    public static final String VENDOR_ID = "vendorId";
    public static final String PRODUCT_ID = "productId";
    public static final String MANUFACTURER = "manufacturer";
    public static final String PRODUCT = "product";
    public static final Set<String> SUPPORTED_PROPERTIES = Set.of(VENDOR_ID, MANUFACTURER, PRODUCT_ID, PRODUCT);

    private final Logger logger = LoggerFactory.getLogger(UsbAddonFinder.class);
    private final Set<UsbSerialDiscovery> usbSerialDiscoveries = new CopyOnWriteArraySet<>();
    private final Set<UsbSerialDeviceInformation> usbDeviceInformations = new CopyOnWriteArraySet<>();

    @Activate
    public UsbAddonFinder() {
    }

    @Deactivate
    public void deactivate() {
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC)
    protected void addUsbSerialDiscovery(UsbSerialDiscovery usbSerialDiscovery) {
        usbSerialDiscoveries.add(usbSerialDiscovery);
        usbSerialDiscovery.registerDiscoveryListener(this);
        usbSerialDiscovery.doSingleScan();
    }

    protected synchronized void removeUsbSerialDiscovery(UsbSerialDiscovery usbSerialDiscovery) {
        usbSerialDiscovery.unregisterDiscoveryListener(this);
        usbSerialDiscoveries.remove(usbSerialDiscovery);
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
                for (UsbSerialDeviceInformation device : usbDeviceInformations) {
                    logger.trace("Checking device: {}", device);

                    String vendorId = toHexString(device.getVendorId());
                    String productId = toHexString(device.getProductId());

                    String manufacturer = device.getManufacturer();
                    String product = device.getProduct();

                    if (propertyMatches(matchProperties, VENDOR_ID, vendorId)
                            && propertyMatches(matchProperties, PRODUCT_ID, productId)
                            && propertyMatches(matchProperties, MANUFACTURER, manufacturer)
                            && propertyMatches(matchProperties, PRODUCT, product)) {
                        result.add(candidate);
                        logger.debug("Suggested add-on found: {}", candidate.getUID());
                        break;
                    }
                }
            }
        }
        return result;
    }

    private String toHexString(int value) {
        return String.format("%04x", value);
    }

    @Override
    public String getServiceName() {
        return SERVICE_NAME;
    }

    @Override
    public void usbSerialDeviceDiscovered(UsbSerialDeviceInformation usbSerialDeviceInformation) {
        usbDeviceInformations.add(usbSerialDeviceInformation);
    }

    @Override
    public void usbSerialDeviceRemoved(UsbSerialDeviceInformation usbSerialDeviceInformation) {
        usbDeviceInformations.remove(usbSerialDeviceInformation);
    }

    public Set<UsbSerialDeviceInformation> getDeviceInformations() {
        return usbDeviceInformations;
    }
}
