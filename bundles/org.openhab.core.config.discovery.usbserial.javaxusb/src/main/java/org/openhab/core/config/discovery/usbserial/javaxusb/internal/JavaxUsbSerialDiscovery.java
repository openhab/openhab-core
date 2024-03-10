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
package org.openhab.core.config.discovery.usbserial.javaxusb.internal;

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

import javax.usb.UsbConfiguration;
import javax.usb.UsbDevice;
import javax.usb.UsbDeviceDescriptor;
import javax.usb.UsbDisconnectedException;
import javax.usb.UsbException;
import javax.usb.UsbHostManager;
import javax.usb.UsbHub;
import javax.usb.UsbInterface;

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

import com.sun.jna.Platform;

/**
 * This is a {@link UsbSerialDiscovery} implementation component that scans the system for USB devices by means of the
 * {@link org.usb4java} library implementation of the {@link javax.usb} interface.
 * <p>
 * It provides USB coverage on non Linux Operating Systems. Linux is already better covered by the scanners in the
 * {@link org.openhab.core.config.discovery.usbserial.linuxsysfs} module.
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
@NonNullByDefault
@Component(service = UsbSerialDiscovery.class, name = JavaxUsbSerialDiscovery.SERVICE_NAME)
public class JavaxUsbSerialDiscovery implements UsbSerialDiscovery {

    protected static final String SERVICE_NAME = "usb-serial-discovery-javaxusb";

    private final Logger logger = LoggerFactory.getLogger(JavaxUsbSerialDiscovery.class);
    private final Set<UsbSerialDiscoveryListener> discoveryListeners = new CopyOnWriteArraySet<>();
    private final Duration scanInterval = Duration.ofSeconds(15);
    private final ScheduledExecutorService scheduler;

    private Set<UsbSerialDeviceInformation> lastScanResult = new HashSet<>();
    private @Nullable ScheduledFuture<?> scanTask;

    @Activate
    public JavaxUsbSerialDiscovery() {
        scheduler = Executors.newSingleThreadScheduledExecutor(
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
                    manufacturer = usbDevice.getString(d.iManufacturer());
                    product = usbDevice.getString(d.iProduct());
                    serialNumber = usbDevice.getString(d.iSerialNumber());
                } catch (UnsupportedEncodingException | UsbDisconnectedException | UsbException e) {
                    // ignore because this would be a 'normal' runtime failure
                }

                String serialPort = "";
                int interfaceNumber = 0;
                String interfaceDescription = null;

                UsbConfiguration configuration = usbDevice.getActiveUsbConfiguration();
                if (configuration == null) {
                    UsbSerialDeviceInformation usbDeviceInfo = new UsbSerialDeviceInformation(vendorId, productId,
                            serialNumber, manufacturer, product, interfaceNumber, interfaceDescription, serialPort);

                    result.add(usbDeviceInfo);
                    logger.trace("Added device: {}", usbDeviceInfo);
                } else {
                    @SuppressWarnings("unchecked")
                    List<UsbInterface> interfaces = configuration.getUsbInterfaces();
                    for (UsbInterface ifx : interfaces) {
                        try {
                            interfaceDescription = ifx.getInterfaceString();
                        } catch (UnsupportedEncodingException | UsbDisconnectedException | UsbException e) {
                            interfaceDescription = null;
                        }

                        UsbSerialDeviceInformation usbDeviceInfo = new UsbSerialDeviceInformation(vendorId, productId,
                                serialNumber, manufacturer, product, interfaceNumber, interfaceDescription, serialPort);

                        result.add(usbDeviceInfo);
                        logger.trace("Added device: {}", usbDeviceInfo);
                        interfaceNumber++;
                    }
                }
            }
        });

        return result;
    }

    @Override
    public synchronized void startBackgroundScanning() {
        if (Platform.isWindows() || Platform.isLinux()) {
            return;
        }
        ScheduledFuture<?> scanTask = this.scanTask;
        if (scanTask == null || scanTask.isDone()) {
            this.scanTask = scheduler.scheduleWithFixedDelay(() -> doSingleScan(), 0, scanInterval.toSeconds(),
                    TimeUnit.SECONDS);
        }
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
