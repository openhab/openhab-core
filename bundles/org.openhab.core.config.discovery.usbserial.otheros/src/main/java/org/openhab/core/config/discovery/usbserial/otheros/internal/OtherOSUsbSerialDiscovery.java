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
package org.openhab.core.config.discovery.usbserial.otheros.internal;

import java.io.UnsupportedEncodingException;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.usb.UsbDevice;
import javax.usb.UsbDeviceDescriptor;
import javax.usb.UsbDisconnectedException;
import javax.usb.UsbException;
import javax.usb.UsbHostManager;
import javax.usb.UsbHub;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.ThreadFactoryBuilder;
import org.openhab.core.config.discovery.usbserial.UsbSerialDeviceInformation;
import org.openhab.core.config.discovery.usbserial.UsbSerialDiscovery;
import org.openhab.core.config.discovery.usbserial.UsbSerialDiscoveryListener;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a {@link UsbSerialDiscovery} implementation component that scans the system for USB devices by means of the
 * {@link org.usb4java} library implementation of the {@link javax.usb} interface.
 * <p>
 * It provides USB coverage on non Linux Operating Systems. Linux is already better covered by the scanners in the
 * {@link org.openhab.core.config.discovery.usbserial.linuxsysfs} module.
 * <p>
 * 
 * @author Andrew Fiddian-Green - Initial contribution
 */
@NonNullByDefault
@Component(service = UsbSerialDiscovery.class, name = OtherOSUsbSerialDiscovery.SERVICE_NAME)
public class OtherOSUsbSerialDiscovery implements UsbSerialDiscovery {

    protected static final String SERVICE_NAME = "usb-serial-discovery-other-os";

    private final Logger logger = LoggerFactory.getLogger(OtherOSUsbSerialDiscovery.class);
    private final Set<UsbSerialDiscoveryListener> discoveryListeners = new CopyOnWriteArraySet<>();
    private final Duration scanInterval = Duration.ofSeconds(15);
    private final boolean isLinux;
    private final @Nullable ScheduledExecutorService scheduler;

    private @Nullable ScheduledFuture<?> scanTask;
    private Set<UsbSerialDeviceInformation> lastScanResult = new HashSet<>();

    @Activate
    public OtherOSUsbSerialDiscovery() {
        isLinux = System.getProperty("os.name", "unknown").toLowerCase().contains("linux");
        scheduler = isLinux ? null
                : Executors.newSingleThreadScheduledExecutor(
                        ThreadFactoryBuilder.create().withName(SERVICE_NAME).withDaemonThreads(true).build());
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

    @Deactivate
    public void deactivate() {
        stopBackgroundScanning();
        lastScanResult.clear();
    }

    @Override
    public synchronized void doSingleScan() {
        if (isLinux) {
            return;
        }

        Set<UsbSerialDeviceInformation> scanResult = scanAllUsbDevicesInformation();
        Set<UsbSerialDeviceInformation> added = setDifference(scanResult, lastScanResult);
        Set<UsbSerialDeviceInformation> removed = setDifference(lastScanResult, scanResult);
        Set<UsbSerialDeviceInformation> unchanged = setDifference(scanResult, added);

        lastScanResult = scanResult;

        removed.stream().forEach(this::announceRemovedDevice);
        added.stream().forEach(this::announceAddedDevice);
        unchanged.stream().forEach(this::announceAddedDevice);
    }

    private <T> Set<T> setDifference(Set<T> set1, Set<T> set2) {
        Set<T> result = new HashSet<>(set1);
        result.removeAll(set2);
        return result;
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

    /**
     * Traverse the USB tree for devices that are children of the ROOT hub, and return a set of USB device information.
     * 
     * @param usbHub the hub whose children are to be found.
     * @return a set of USB device information.
     */
    private Set<UsbSerialDeviceInformation> scanAllUsbDevicesInformation() {
        try {
            return scanChildUsbDeviceInformation(UsbHostManager.getUsbServices().getRootUsbHub());
        } catch (SecurityException | UsbException e) {
            logger.warn("Error getting USB device information: {}", e.getMessage());
            return Set.of();
        }
    }

    /**
     * Traverse the USB tree for devices that are children of the given hub, and return a set of USB device information.
     * 
     * @param usbHub the hub whose children are to be found.
     * @return a set of USB device information.
     */
    private Set<UsbSerialDeviceInformation> scanChildUsbDeviceInformation(UsbHub usbHub) {
        Set<UsbSerialDeviceInformation> result = new HashSet<>();

        @SuppressWarnings("unchecked")
        List<UsbDevice> deviceList = usbHub.getAttachedUsbDevices();

        deviceList.forEach(usbDevice -> {
            if (usbDevice.isUsbHub()) {
                result.addAll(scanChildUsbDeviceInformation((UsbHub) usbDevice));
            } else {
                UsbDeviceDescriptor d = usbDevice.getUsbDeviceDescriptor();
                short vendorId = d.idVendor();
                short productId = d.idProduct();

                String manufacturer = null;
                String product = null;
                String serialNumber = null;

                /*
                 * Note: the getString() calls below may fail depending on the Operating System:
                 * - on Windows if no libusb device driver is installed for the device.
                 * - on Linux if the user has no write permission on the USB device file.
                 */
                try {
                    product = usbDevice.getString(d.iProduct());
                    manufacturer = usbDevice.getString(d.iManufacturer());
                    serialNumber = usbDevice.getString(d.iSerialNumber());
                } catch (UnsupportedEncodingException | UsbDisconnectedException | UsbException e) {
                    // ignore because this would be a 'normal' runtime failure
                }

                if (product == null || product.isBlank()) {
                    product = UsbProductDatabase.getProduct(vendorId, productId);
                }

                /*
                 * TODO are the following three data required (??)
                 */
                int interfaceNumber = 0;
                String interfaceDescription = "n/a";
                String serialPort = "n/a";

                UsbSerialDeviceInformation usbDeviceInfo = new UsbSerialDeviceInformation(vendorId, productId,
                        serialNumber, manufacturer, product, interfaceNumber, interfaceDescription, serialPort);

                result.add(usbDeviceInfo);
                logger.trace("Added device: {}", usbDeviceInfo);
            }
        });

        return result;
    }

    @Override
    public synchronized void startBackgroundScanning() {
        scanTask = scheduler.scheduleWithFixedDelay(() -> doSingleScan(), 0, scanInterval.toSeconds(),
                TimeUnit.SECONDS);
    }

    @Override
    public synchronized void stopBackgroundScanning() {
        ScheduledFuture<?> scanTask = this.scanTask;
        if (scanTask != null) {
            scanTask.cancel(false);
        }
        this.scanTask = null;
    }
}
