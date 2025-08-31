/*
 * Copyright (c) 2010-2025 Contributors to the openHAB project
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
import com.sun.jna.platform.win32.Win32Exception;

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
    private static final String KEY_DEVICE_PARAMETERS = "Device Parameters";
    private static final String KEY_SERIAL_PORT = "PortName";

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

        removed.forEach(this::announceRemovedDevice);
        added.forEach(this::announceAddedDevice);
        unchanged.forEach(this::announceAddedDevice);
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
            return new HashSet<>();
        }

        Set<UsbSerialDeviceInformation> result = new HashSet<>();
        String[] deviceKeys;
        try {
            deviceKeys = Advapi32Util.registryGetKeys(HKEY_LOCAL_MACHINE, USB_REGISTRY_ROOT);
        } catch (Win32Exception e) {
            logger.debug("registryGetKeys failed for {}", USB_REGISTRY_ROOT, e);
            return result;
        }

        for (String deviceKey : deviceKeys) {
            logger.trace("{}", deviceKey);

            if (!deviceKey.startsWith(PREFIX_VID)) {
                continue;
            }

            String[] ids = deviceKey.split(SPLIT_IDS);
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

            String serialNumber = ids.length > 2 ? ids[2] : null;

            String devicePath = USB_REGISTRY_ROOT + BACKSLASH + deviceKey;
            String[] interfaceNames;
            try {
                interfaceNames = Advapi32Util.registryGetKeys(HKEY_LOCAL_MACHINE, devicePath);
            } catch (Win32Exception e) {
                logger.debug("registryGetKeys failed for {}", devicePath, e);
                continue;
            }

            int interfaceId = 0;
            for (String interfaceName : interfaceNames) {
                logger.trace("  interfaceId:{}, interfaceName:{}", interfaceId, interfaceName);

                String interfacePath = devicePath + BACKSLASH + interfaceName;
                TreeMap<String, Object> values;
                try {
                    values = Advapi32Util.registryGetValues(HKEY_LOCAL_MACHINE, interfacePath);
                } catch (Win32Exception e) {
                    logger.debug("registryGetValues failed for {}", interfacePath, e);
                    continue;
                }

                if (logger.isTraceEnabled()) {
                    for (Entry<String, Object> value : values.entrySet()) {
                        logger.trace("    {}={}", value.getKey(), value.getValue());
                    }
                }

                String manufacturer;
                Object manufacturerValue = values.get(KEY_MANUFACTURER);
                if (manufacturerValue instanceof String manufacturerString) {
                    String[] manufacturerData = manufacturerString.split(SPLIT_VALUES);
                    if (manufacturerData.length < 2) {
                        continue;
                    }
                    manufacturer = manufacturerData[1];
                } else {
                    continue;
                }

                String product;
                Object productValue = values.get(KEY_PRODUCT);
                if (productValue instanceof String productString) {
                    String[] productData = productString.split(SPLIT_VALUES);
                    if (productData.length < 2) {
                        continue;
                    }
                    product = productData[1];
                } else {
                    continue;
                }

                String serialPort = "";
                String[] interfaceSubKeys;
                try {
                    interfaceSubKeys = Advapi32Util.registryGetKeys(HKEY_LOCAL_MACHINE, interfacePath);
                } catch (Win32Exception e) {
                    logger.debug("registryGetKeys failed for {}", interfacePath, e);
                    continue;
                }

                for (String interfaceSubKey : interfaceSubKeys) {
                    if (!KEY_DEVICE_PARAMETERS.equals(interfaceSubKey)) {
                        continue;
                    }
                    String deviceParametersPath = interfacePath + BACKSLASH + interfaceSubKey;
                    TreeMap<String, Object> deviceParameterValues;
                    try {
                        deviceParameterValues = Advapi32Util.registryGetValues(HKEY_LOCAL_MACHINE,
                                deviceParametersPath);
                    } catch (Win32Exception e) {
                        logger.debug("registryGetValues failed for {}", deviceParametersPath, e);
                        continue;
                    }
                    Object serialPortValue = deviceParameterValues.get(KEY_SERIAL_PORT);
                    if (serialPortValue instanceof String serialPortString) {
                        serialPort = serialPortString;
                    }
                    break;
                }

                UsbSerialDeviceInformation usbSerialDeviceInformation = new UsbSerialDeviceInformation(vendorId,
                        productId, serialNumber, manufacturer, product, interfaceId, interfaceName, serialPort);

                logger.debug("Add {}", usbSerialDeviceInformation);
                result.add(usbSerialDeviceInformation);

                interfaceId++;
            }
        }
        return result;
    }

    @Override
    public synchronized void startBackgroundScanning() {
        if (Platform.isWindows()) {
            ScheduledFuture<?> scanTask = this.scanTask;
            if (scanTask == null || scanTask.isDone()) {
                this.scanTask = scheduler.scheduleWithFixedDelay(this::doSingleScan, 0, scanInterval.toSeconds(),
                        TimeUnit.SECONDS);
            }
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
