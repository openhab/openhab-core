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

import static com.sun.jna.platform.win32.WinReg.HKEY_LOCAL_MACHINE;

import java.time.Duration;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
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

import com.sun.jna.Platform;
import com.sun.jna.platform.win32.Advapi32Util;

/**
 * This is a {@link UsbSerialDiscovery} implementation component for Windows.
 * It parses the Windows registry for USB device entries.
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
@NonNullByDefault
@Component(service = UsbSerialDiscovery.class, name = WindowsUsbSerialDiscovery.SERVICE_NAME)
public class WindowsUsbSerialDiscovery implements UsbSerialDiscovery {

    protected static final String SERVICE_NAME = "usb-serial-discovery-windows";

    // registry accessor strings
    private static final String USB_REGISTRY_ROOT = "SYSTEM\\CurrentControlSet\\Enum\\USB";
    private static final String BACKSLASH = "\\";
    private static final String PREFIX_PID = "PID_";
    private static final String PREFIX_VID = "VID_";
    private static final String PREFIX_HEX = "0x";
    private static final String SPLIT_IDS = "&";
    private static final String SPLIT_VALUES = ";";
    private static final String KEY_MANUFACTURER = "Mfg";
    private static final String KEY_PRODUCT = "DeviceDesc";

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
     * Traverse the USB tree in Windows registry and return a set of USB device information.
     *
     * @return a set of USB device information.
     */
    public Set<UsbSerialDeviceInformation> scanAllUsbDevicesInformation() {
        if (!Platform.isWindows()) {
            return Set.of();
        }

        Set<UsbSerialDeviceInformation> result = new HashSet<>();
        String[] usbKeys = Advapi32Util.registryGetKeys(HKEY_LOCAL_MACHINE, USB_REGISTRY_ROOT);

        for (String usbKey : usbKeys) {
            logger.trace("{}", usbKey);

            if (!usbKey.startsWith(PREFIX_VID)) {
                continue;
            }

            String[] ids = usbKey.split(SPLIT_IDS);
            if (ids.length < 2) {
                continue;
            }

            if (!ids[1].startsWith(PREFIX_PID)) {
                continue;
            }

            int vendorId;
            int productId;
            try {
                vendorId = Integer.decode(PREFIX_HEX + ids[0].substring(4));
                productId = Integer.decode(PREFIX_HEX + ids[1].substring(4));
            } catch (NumberFormatException e) {
                continue;
            }

            String usbPath = USB_REGISTRY_ROOT + BACKSLASH + usbKey;
            String[] usbSubKeys = Advapi32Util.registryGetKeys(HKEY_LOCAL_MACHINE, usbPath);

            for (String usbSubKey : usbSubKeys) {
                logger.trace("  {}", usbSubKey);

                String usbSubPath = usbPath + BACKSLASH + usbSubKey;
                TreeMap<String, Object> values = Advapi32Util.registryGetValues(HKEY_LOCAL_MACHINE, usbSubPath);

                if (logger.isTraceEnabled()) {
                    for (Entry<String, Object> value : values.entrySet()) {
                        logger.trace("    {}={}", value.getKey(), value.getValue());
                    }
                }

                String manufacturer;
                Object manufacturerObject = values.get(KEY_MANUFACTURER);
                if (manufacturerObject instanceof String manufacturerString) {
                    String[] manufacturerData = manufacturerString.split(SPLIT_VALUES);
                    if (manufacturerData.length < 2) {
                        continue;
                    }
                    manufacturer = manufacturerData[1];
                } else {
                    continue;
                }

                String product;
                Object productObject = values.get(KEY_PRODUCT);
                if (productObject instanceof String productString) {
                    String[] productData = productString.split(SPLIT_VALUES);
                    if (productData.length < 2) {
                        continue;
                    }
                    product = productData[1];
                } else {
                    continue;
                }

                UsbSerialDeviceInformation usbSerialDeviceInformation = new UsbSerialDeviceInformation(vendorId,
                        productId, null, manufacturer, product, 0, null, "");

                logger.debug("Add {}", usbSerialDeviceInformation);
                result.add(usbSerialDeviceInformation);
                break;
            }
        }
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
