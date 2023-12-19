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
package org.openhab.core.config.discovery.usbserial.windowsregistry.internal;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

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
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
@NonNullByDefault
@Component(service = UsbSerialDiscovery.class, name = WindowsUsbSerialDiscovery.SERVICE_NAME)
public class WindowsUsbSerialDiscovery implements UsbSerialDiscovery {

    protected static final String SERVICE_NAME = "usb-serial-discovery-windows";

    private final Logger logger = LoggerFactory.getLogger(WindowsUsbSerialDiscovery.class);
    private final Set<UsbSerialDiscoveryListener> discoveryListeners = new CopyOnWriteArraySet<>();
    private final Duration scanInterval = Duration.ofSeconds(15);
    private final ScheduledExecutorService scheduler;

    private Set<UsbSerialDeviceInformation> lastScanResult = new HashSet<>();
    private @Nullable ScheduledFuture<?> scanTask;

    @Activate
    public WindowsUsbSerialDiscovery() {
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
     * Traverse the USB tree and return a set of USB device information.
     *
     * @return a set of USB device information.
     */
    private Set<UsbSerialDeviceInformation> scanAllUsbDevicesInformation() {
        return Set.of();
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
