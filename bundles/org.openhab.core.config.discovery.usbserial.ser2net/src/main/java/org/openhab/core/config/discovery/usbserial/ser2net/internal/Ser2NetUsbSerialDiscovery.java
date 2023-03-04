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
package org.openhab.core.config.discovery.usbserial.ser2net.internal;

import java.time.Duration;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.config.discovery.usbserial.UsbSerialDeviceInformation;
import org.openhab.core.config.discovery.usbserial.UsbSerialDiscovery;
import org.openhab.core.config.discovery.usbserial.UsbSerialDiscoveryListener;
import org.openhab.core.io.transport.mdns.MDNSClient;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link UsbSerialDiscovery} that implements background discovery of RFC2217 by listening to
 * ser2net mDNS service events.
 *
 * @author Wouter Born - Initial contribution
 */
@NonNullByDefault
@Component(service = UsbSerialDiscovery.class)
public class Ser2NetUsbSerialDiscovery implements ServiceListener, UsbSerialDiscovery {

    private final Logger logger = LoggerFactory.getLogger(Ser2NetUsbSerialDiscovery.class);

    static final String SERVICE_TYPE = "_iostream._tcp.local.";

    static final String PROPERTY_PROVIDER = "provider";
    static final String PROPERTY_DEVICE_TYPE = "devicetype";
    static final String PROPERTY_GENSIO_STACK = "gensiostack";

    static final String PROPERTY_VENDOR_ID = "idVendor";
    static final String PROPERTY_PRODUCT_ID = "idProduct";

    static final String PROPERTY_SERIAL_NUMBER = "serial";
    static final String PROPERTY_MANUFACTURER = "manufacturer";
    static final String PROPERTY_PRODUCT = "product";

    static final String PROPERTY_INTERFACE_NUMBER = "bInterfaceNumber";
    static final String PROPERTY_INTERFACE = "interface";

    static final String SER2NET = "ser2net";
    static final String SERIALUSB = "serialusb";
    static final String TELNET_RFC2217_TCP = "telnet(rfc2217),tcp";

    static final Duration SINGLE_SCAN_DURATION = Duration.ofSeconds(5);
    static final String SERIAL_PORT_NAME_FORMAT = "rfc2217://%s:%s";

    private final Set<UsbSerialDiscoveryListener> discoveryListeners = new CopyOnWriteArraySet<>();
    private final MDNSClient mdnsClient;

    private boolean notifyListeners = false;

    private Set<UsbSerialDeviceInformation> lastScanResult = new HashSet<>();

    @Activate
    public Ser2NetUsbSerialDiscovery(final @Reference MDNSClient mdnsClient) {
        this.mdnsClient = mdnsClient;
    }

    @Override
    public void registerDiscoveryListener(UsbSerialDiscoveryListener listener) {
        discoveryListeners.add(listener);
        for (UsbSerialDeviceInformation deviceInfo : lastScanResult) {
            listener.usbSerialDeviceDiscovered(deviceInfo);
        }
    }

    @Override
    public void unregisterDiscoveryListener(UsbSerialDiscoveryListener listener) {
        discoveryListeners.remove(listener);
    }

    @Override
    public synchronized void startBackgroundScanning() {
        notifyListeners = true;
        mdnsClient.addServiceListener(SERVICE_TYPE, this);
        logger.debug("Started ser2net USB-Serial mDNS background discovery");
    }

    @Override
    public synchronized void stopBackgroundScanning() {
        notifyListeners = false;
        mdnsClient.removeServiceListener(SERVICE_TYPE, this);
        logger.debug("Stopped ser2net USB-Serial mDNS background discovery");
    }

    @Override
    public synchronized void doSingleScan() {
        logger.debug("Starting ser2net USB-Serial mDNS single discovery scan");

        Set<UsbSerialDeviceInformation> scanResult = Stream.of(mdnsClient.list(SERVICE_TYPE, SINGLE_SCAN_DURATION))
                .map(this::createUsbSerialDeviceInformation) //
                .filter(Optional::isPresent) //
                .map(Optional::get) //
                .collect(Collectors.toSet());

        Set<UsbSerialDeviceInformation> added = setDifference(scanResult, lastScanResult);
        Set<UsbSerialDeviceInformation> removed = setDifference(lastScanResult, scanResult);
        Set<UsbSerialDeviceInformation> unchanged = setDifference(scanResult, added);

        lastScanResult = scanResult;

        removed.stream().forEach(this::announceRemovedDevice);
        added.stream().forEach(this::announceAddedDevice);
        unchanged.stream().forEach(this::announceAddedDevice);

        logger.debug("Completed ser2net USB-Serial mDNS single discovery scan");
    }

    private <T> Set<T> setDifference(Set<T> set1, Set<T> set2) {
        Set<T> result = new HashSet<>(set1);
        result.removeAll(set2);
        return result;
    }

    private void announceAddedDevice(UsbSerialDeviceInformation deviceInfo) {
        for (UsbSerialDiscoveryListener listener : discoveryListeners) {
            listener.usbSerialDeviceDiscovered(deviceInfo);
        }
    }

    private void announceRemovedDevice(UsbSerialDeviceInformation deviceInfo) {
        for (UsbSerialDiscoveryListener listener : discoveryListeners) {
            listener.usbSerialDeviceRemoved(deviceInfo);
        }
    }

    @Override
    public void serviceAdded(@NonNullByDefault({}) ServiceEvent event) {
        if (notifyListeners) {
            Optional<UsbSerialDeviceInformation> deviceInfo = createUsbSerialDeviceInformation(event.getInfo());
            deviceInfo.ifPresent(this::announceAddedDevice);
        }
    }

    @Override
    public void serviceRemoved(@NonNullByDefault({}) ServiceEvent event) {
        if (notifyListeners) {
            Optional<UsbSerialDeviceInformation> deviceInfo = createUsbSerialDeviceInformation(event.getInfo());
            deviceInfo.ifPresent(this::announceRemovedDevice);
        }
    }

    @Override
    public void serviceResolved(@NonNullByDefault({}) ServiceEvent event) {
        serviceAdded(event);
    }

    private Optional<UsbSerialDeviceInformation> createUsbSerialDeviceInformation(ServiceInfo serviceInfo) {
        String provider = serviceInfo.getPropertyString(PROPERTY_PROVIDER);
        String deviceType = serviceInfo.getPropertyString(PROPERTY_DEVICE_TYPE);
        String gensioStack = serviceInfo.getPropertyString(PROPERTY_GENSIO_STACK);

        // Check ser2net specific properties when present
        if (SER2NET.equals(provider) && (deviceType != null && !SERIALUSB.equals(deviceType))
                || (gensioStack != null && !TELNET_RFC2217_TCP.equals(gensioStack))) {
            logger.debug("Skipping creation of UsbSerialDeviceInformation based on {}", serviceInfo);
            return Optional.empty();
        }

        try {
            int vendorId = Integer.parseInt(serviceInfo.getPropertyString(PROPERTY_VENDOR_ID), 16);
            int productId = Integer.parseInt(serviceInfo.getPropertyString(PROPERTY_PRODUCT_ID), 16);

            String serialNumber = serviceInfo.getPropertyString(PROPERTY_SERIAL_NUMBER);
            String manufacturer = serviceInfo.getPropertyString(PROPERTY_MANUFACTURER);
            String product = serviceInfo.getPropertyString(PROPERTY_PRODUCT);

            int interfaceNumber = Integer.parseInt(serviceInfo.getPropertyString(PROPERTY_INTERFACE_NUMBER), 16);
            String interfaceDescription = serviceInfo.getPropertyString(PROPERTY_INTERFACE);

            String serialPortName = String.format(SERIAL_PORT_NAME_FORMAT, serviceInfo.getHostAddresses()[0],
                    serviceInfo.getPort());

            UsbSerialDeviceInformation deviceInfo = new UsbSerialDeviceInformation(vendorId, productId, serialNumber,
                    manufacturer, product, interfaceNumber, interfaceDescription, serialPortName);
            logger.debug("Created {} based on {}", deviceInfo, serviceInfo);
            return Optional.of(deviceInfo);
        } catch (NumberFormatException e) {
            logger.debug("Failed to create UsbSerialDeviceInformation based on {}", serviceInfo, e);
            return Optional.empty();
        }
    }
}
