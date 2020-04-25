/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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
package org.openhab.core.config.discovery.usbserial.linuxsysfs.internal;

import static java.lang.Long.parseLong;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
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
import org.openhab.core.config.discovery.usbserial.linuxsysfs.internal.DeltaUsbSerialScanner.Delta;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link UsbSerialDiscovery} that implements background discovery by doing repetitive scans using a
 * {@link UsbSerialScanner}, pausing a configurable amount of time between subsequent scans.
 *
 * @author Henning Sudbrock - Initial contribution
 */
@NonNullByDefault
@Component(configurationPid = "discovery.usbserial.linuxsysfs.pollingscanner")
public class PollingUsbSerialScanner implements UsbSerialDiscovery {

    private final Logger logger = LoggerFactory.getLogger(PollingUsbSerialScanner.class);

    private static final String THREAD_NAME = "usb-serial-discovery-linux-sysfs";

    public static final String PAUSE_BETWEEN_SCANS_IN_SECONDS_ATTRIBUTE = "pauseBetweenScansInSeconds";
    private static final Duration DEFAULT_PAUSE_BETWEEN_SCANS = Duration.ofSeconds(15);
    private Duration pauseBetweenScans = DEFAULT_PAUSE_BETWEEN_SCANS;

    private @NonNullByDefault({}) DeltaUsbSerialScanner deltaUsbSerialScanner;

    private final Set<UsbSerialDiscoveryListener> discoveryListeners = new CopyOnWriteArraySet<>();

    private @NonNullByDefault({}) ScheduledExecutorService scheduler;

    private @Nullable ScheduledFuture<?> backgroundScanningJob;

    @Reference
    protected void setUsbSerialScanner(UsbSerialScanner usbSerialScanner) {
        deltaUsbSerialScanner = new DeltaUsbSerialScanner(usbSerialScanner);
    }

    protected void unsetUsbSerialScanner(UsbSerialScanner usbSerialScanner) {
        deltaUsbSerialScanner = null;
    }

    @Activate
    protected void activate(Map<String, Object> config) {
        if (config.containsKey(PAUSE_BETWEEN_SCANS_IN_SECONDS_ATTRIBUTE)) {
            pauseBetweenScans = Duration
                    .ofSeconds(parseLong(config.get(PAUSE_BETWEEN_SCANS_IN_SECONDS_ATTRIBUTE).toString()));
        }

        scheduler = Executors.newSingleThreadScheduledExecutor(
                ThreadFactoryBuilder.create().withName(THREAD_NAME).withDaemonThreads(true).build());
    }

    @Deactivate
    protected void deactivate() {
        scheduler.shutdown();
    }

    @Modified
    protected synchronized void modified(Map<String, Object> config) {
        if (config.containsKey(PAUSE_BETWEEN_SCANS_IN_SECONDS_ATTRIBUTE)) {
            pauseBetweenScans = Duration
                    .ofSeconds(parseLong(config.get(PAUSE_BETWEEN_SCANS_IN_SECONDS_ATTRIBUTE).toString()));

            if (backgroundScanningJob != null) {
                stopBackgroundScanning();
                startBackgroundScanning();
            }
        }
    }

    /**
     * Performs a single scan for newly added and removed devices.
     */
    @Override
    public void doSingleScan() {
        singleScanInternal(true);
    }

    /**
     * Starts repeatedly scanning for newly added and removed USB devices using the configured {@link UsbSerialScanner}
     * (where the duration between two subsequent scans is configurable).
     * <p/>
     * This repeated scanning can be stopped using {@link #stopBackgroundScanning()}.
     */
    @Override
    public synchronized void startBackgroundScanning() {
        if (backgroundScanningJob == null) {
            if (deltaUsbSerialScanner.canPerformScans()) {
                backgroundScanningJob = scheduler.scheduleWithFixedDelay(() -> {
                    singleScanInternal(false);
                }, 0, pauseBetweenScans.getSeconds(), TimeUnit.SECONDS);
                logger.debug("Scheduled USB-Serial background discovery every {} seconds",
                        pauseBetweenScans.getSeconds());
            } else {
                logger.debug(
                        "Do not start background scanning, as the configured USB-Serial scanner cannot perform scans on this system");
            }
        }
    }

    /**
     * Stops repeatedly scanning for newly added and removed USB devices. This can be restarted using
     * {@link #startBackgroundScanning()}.
     */
    @Override
    public synchronized void stopBackgroundScanning() {
        logger.debug("Stopping USB-Serial background discovery");
        ScheduledFuture<?> currentBackgroundScanningJob = backgroundScanningJob;
        if (currentBackgroundScanningJob != null && !currentBackgroundScanningJob.isCancelled()) {
            if (currentBackgroundScanningJob.cancel(true)) {
                backgroundScanningJob = null;
                logger.debug("Stopped USB-serial background discovery");
            }
        }
    }

    @Override
    public void registerDiscoveryListener(UsbSerialDiscoveryListener listener) {
        discoveryListeners.add(listener);
    }

    @Override
    public void unregisterDiscoveryListener(UsbSerialDiscoveryListener listener) {
        discoveryListeners.remove(listener);
    }

    private void singleScanInternal(boolean announceUnchangedDevices) {
        try {
            Delta<UsbSerialDeviceInformation> delta = deltaUsbSerialScanner.scan();
            announceAddedDevices(delta.getAdded());
            announceRemovedDevices(delta.getRemoved());
            if (announceUnchangedDevices) {
                announceAddedDevices(delta.getUnchanged());
            }
        } catch (IOException e) {
            logger.debug("A {} prevented a scan for USB serial devices: {}", e.getClass().getSimpleName(),
                    e.getMessage());
        }
    }

    private void announceAddedDevices(Set<UsbSerialDeviceInformation> deviceInfos) {
        for (UsbSerialDeviceInformation deviceInfo : deviceInfos) {
            for (UsbSerialDiscoveryListener listener : discoveryListeners) {
                listener.usbSerialDeviceDiscovered(deviceInfo);
            }
        }
    }

    private void announceRemovedDevices(Set<UsbSerialDeviceInformation> deviceInfos) {
        for (UsbSerialDeviceInformation deviceInfo : deviceInfos) {
            for (UsbSerialDiscoveryListener listener : discoveryListeners) {
                listener.usbSerialDeviceRemoved(deviceInfo);
            }
        }
    }
}
