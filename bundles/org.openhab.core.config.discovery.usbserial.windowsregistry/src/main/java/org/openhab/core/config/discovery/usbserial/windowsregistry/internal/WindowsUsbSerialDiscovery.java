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
package org.openhab.core.config.discovery.usbserial.windowsregistry.internal;

import static java.lang.Long.parseLong;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.common.ThreadFactoryBuilder;
import org.openhab.core.config.discovery.usbserial.UsbSerialDeviceInformation;
import org.openhab.core.config.discovery.usbserial.UsbSerialDiscovery;
import org.openhab.core.config.discovery.usbserial.UsbSerialDiscoveryListener;
import org.openhab.core.config.discovery.usbserial.windowsregistry.internal.WindowMessageHandler.WindowMessageListener;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.platform.win32.Advapi32;
import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.Guid.GUID;
import com.sun.jna.platform.win32.Kernel32Util;
import com.sun.jna.platform.win32.SetupApi;
import com.sun.jna.platform.win32.SetupApi.SP_DEVICE_INTERFACE_DATA;
import com.sun.jna.platform.win32.SetupApi.SP_DEVINFO_DATA;
import com.sun.jna.platform.win32.Win32Exception;
import com.sun.jna.platform.win32.WinBase;
import com.sun.jna.platform.win32.WinError;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.WinNT.HANDLE;
import com.sun.jna.platform.win32.WinReg;
import com.sun.jna.ptr.IntByReference;

/**
 * This is a {@link UsbSerialDiscovery} implementation component for Windows. It uses the Windows API to query for and
 * be notified of USB devices. It will attempt to subscribe to device change notifications by creating an invisible
 * window for that subscribes to changes. If that fails, it will fall back to interval scanning.
 *
 * @author Andrew Fiddian-Green - Initial contribution
 * @author Ravi Nadahar - Refactor to use SetupApi
 */
@NonNullByDefault
@Component(service = UsbSerialDiscovery.class, name = WindowsUsbSerialDiscovery.SERVICE_NAME, configurationPid = "discovery.usbserial.windows")
public class WindowsUsbSerialDiscovery implements UsbSerialDiscovery, WindowMessageListener {

    protected static final String SERVICE_NAME = "usb-serial-discovery-windows";
    public static final String SCAN_INTERVAL_PROPERTY = "scanInterval";
    public static final int DEFAULT_SCAN_INTERVAL_SECONDS = 15;
    private static final String DEVICE_PATH_PATTERN = "^\\\\\\\\\\?\\\\usb#vid_(?<vid>[0-9a-f]{4})&pid_(?<pid>[0-9a-f]{4})(?:&mi_(?<mi>[0-9a-f]{2}))?#(?<id>.*?)(?:#(?<guid>\\{[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\}))$";

    private final Pattern devicePathPattern = Pattern.compile(DEVICE_PATH_PATTERN);

    private record DevicePathData(int vendorId, int productId, String id, int interfaceNumber) {
    }

    private static final boolean IS_64_BIT = Platform.is64Bit();
    private static final int ERROR_NO_SUCH_DEVINST = 0xe000020b;

    // registry accessor strings
    private static final String KEY_SERIAL_PORT = "PortName";

    private static final GUID GUID_DEVINTERFACE_USB_DEVICE = new GUID("A5DCBF10-6530-11D2-901F-00C04FB951ED");
    private static final int SPDRP_FRIENDLYNAME = 0x0000000C;
    private static final int SPDRP_MFG = 0x0000000B;

    private final Logger logger = LoggerFactory.getLogger(WindowsUsbSerialDiscovery.class);
    private final Set<UsbSerialDiscoveryListener> discoveryListeners = new CopyOnWriteArraySet<>();
    private volatile Duration scanInterval = Duration.ofSeconds(DEFAULT_SCAN_INTERVAL_SECONDS);
    private final ScheduledExecutorService scheduler;

    // All access must be guarded by "this"
    private Set<UsbSerialDeviceInformation> lastScanResult = new HashSet<>();

    // All access must be guarded by "this"
    private @Nullable ScheduledFuture<?> scanTask;

    // All access must be guarded by "this"
    private @Nullable WindowMessageHandler windowMessageHandler;

    /** Indicated that listening for device changes using window messages failed */
    private volatile boolean windowMessageFailed;

    @Activate
    public WindowsUsbSerialDiscovery(Map<String, Object> config) {
        Object value = config.get(SCAN_INTERVAL_PROPERTY);
        if (value instanceof String s) {
            try {
                scanInterval = Duration.ofSeconds(parseLong(s));
            } catch (NumberFormatException e) {
                logger.warn("Invalid configuration value for '{}': {}", SCAN_INTERVAL_PROPERTY, s);
            }
        } else if (value instanceof Number n) {
            scanInterval = Duration.ofSeconds(n.longValue());
        }

        scheduler = Executors.newSingleThreadScheduledExecutor(
                ThreadFactoryBuilder.create().withName(SERVICE_NAME).withDaemonThreads(true).build());
    }

    @Modified
    protected void modified(Map<String, Object> config) {
        Object value = config.get(SCAN_INTERVAL_PROPERTY);
        Duration newScanInterval = null;
        if (value instanceof String s) {
            try {
                newScanInterval = Duration.ofSeconds(parseLong(s));
            } catch (NumberFormatException e) {
                logger.warn("Invalid configuration value for '{}': {}", SCAN_INTERVAL_PROPERTY, s);
            }
        } else if (value instanceof Number n) {
            newScanInterval = Duration.ofSeconds(n.longValue());
        }

        synchronized (this) {
            if (!Objects.equals(newScanInterval, scanInterval)) {
                if (newScanInterval == null) {
                    scanInterval = Duration.ofSeconds(DEFAULT_SCAN_INTERVAL_SECONDS);
                } else {
                    scanInterval = newScanInterval;
                }
                if (scanTask != null) {
                    stopBackgroundScanning();
                    startBackgroundScanning();
                }
            }
        }
    }

    @Deactivate
    public void deactivate() {
        synchronized (this) {
            stopBackgroundScanning();
            lastScanResult.clear();
        }
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
    public void doSingleScan() {
        doSingleScanInternal(true);
    }

    protected void doSingleScanInternal(boolean includeExisting) {
        Set<UsbSerialDeviceInformation> scanResult;
        Set<UsbSerialDeviceInformation> added;
        Set<UsbSerialDeviceInformation> removed;
        Set<UsbSerialDeviceInformation> unchanged;
        synchronized (this) {
            scanResult = gatherUsbDevicesInformation();
            added = setDifference(scanResult, lastScanResult);
            removed = setDifference(lastScanResult, scanResult);
            unchanged = includeExisting ? setDifference(scanResult, added) : Set.of();

            lastScanResult = scanResult;
        }

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
        Set<UsbSerialDeviceInformation> lastScanResult;
        synchronized (this) {
            lastScanResult = Set.copyOf(this.lastScanResult);
        }
        for (UsbSerialDeviceInformation deviceInfo : lastScanResult) {
            listener.usbSerialDeviceDiscovered(deviceInfo);
        }
    }

    @Override
    public void unregisterDiscoveryListener(UsbSerialDiscoveryListener listener) {
        discoveryListeners.remove(listener);
    }

    /**
     * Traverse Windows USB devices and return a set of USB device information.
     *
     * @return a set of USB device information.
     */
    public Set<UsbSerialDeviceInformation> gatherUsbDevicesInformation() {
        if (!Platform.isWindows()) {
            return Set.of();
        }

        SetupApi apiInst = SetupApi.INSTANCE;

        Set<UsbSerialDeviceInformation> result = new HashSet<>();
        HANDLE deviceInfoSet = apiInst.SetupDiGetClassDevs(GUID_DEVINTERFACE_USB_DEVICE, null, null,
                SetupApi.DIGCF_DEVICEINTERFACE | SetupApi.DIGCF_PRESENT);
        String serialPort;
        int lastError;
        if (!WinBase.INVALID_HANDLE_VALUE.equals(deviceInfoSet)) {
            try {
                SP_DEVINFO_DATA deviceInfoData = new SP_DEVINFO_DATA();
                SP_DEVICE_INTERFACE_DATA deviceInterfaceData = new SP_DEVICE_INTERFACE_DATA();

                int devIdx = 0;
                int intIdx;
                while (apiInst.SetupDiEnumDeviceInfo(deviceInfoSet, devIdx, deviceInfoData)) {
                    String name;
                    String friendlyName;
                    String mfg;
                    try {
                        name = getDeviceRegistryPropertyString(deviceInfoSet, SetupApi.SPDRP_DEVICEDESC,
                                deviceInfoData);
                        friendlyName = getDeviceRegistryPropertyString(deviceInfoSet, SPDRP_FRIENDLYNAME,
                                deviceInfoData);
                        mfg = getDeviceRegistryPropertyString(deviceInfoSet, SPDRP_MFG, deviceInfoData);
                    } catch (Win32Exception e) {
                        logger.warn("Failed to get USB device property: {}", e.getMessage());
                        continue;
                    }

                    intIdx = 0;
                    while (apiInst.SetupDiEnumDeviceInterfaces(deviceInfoSet, deviceInfoData.getPointer(),
                            GUID_DEVINTERFACE_USB_DEVICE, intIdx, deviceInterfaceData)) {
                        List<String> devicePaths;
                        try {
                            devicePaths = getDeviceInterfaceDetails(deviceInfoSet, deviceInterfaceData, null);
                        } catch (Win32Exception e) {
                            logger.warn("Failed to get USB device interface details for \"{}\": {}", name,
                                    e.getMessage());
                            continue;
                        }
                        DevicePathData data;
                        for (String devicePath : devicePaths) {
                            data = parseDevicePath(devicePath);
                            if (data != null) {
                                WinReg.HKEY hKey = apiInst.SetupDiOpenDevRegKey(deviceInfoSet, deviceInfoData,
                                        SetupApi.DICS_FLAG_GLOBAL, 0, SetupApi.DIREG_DEV, WinNT.KEY_READ);
                                if (hKey != WinBase.INVALID_HANDLE_VALUE) {
                                    try {
                                        serialPort = Advapi32Util.registryGetStringValue(hKey, KEY_SERIAL_PORT);
                                    } catch (Win32Exception e) {
                                        if (e.getErrorCode() != WinError.ERROR_FILE_NOT_FOUND) {
                                            logger.debug("Failed to read serial port for USB device \"{}\": {} {}",
                                                    name, e.getClass().getSimpleName(), e.getMessage());
                                        }
                                        serialPort = "";
                                    } catch (RuntimeException e) {
                                        logger.debug("Failed to read serial port for USB device \"{}\": {} {}", name,
                                                e.getClass().getSimpleName(), e.getMessage());
                                        serialPort = "";
                                    } finally {
                                        Advapi32.INSTANCE.RegCloseKey(hKey);
                                    }
                                } else {
                                    serialPort = "";
                                }

                                UsbSerialDeviceInformation usbSerialDeviceInformation = new UsbSerialDeviceInformation(
                                        data.vendorId, data.productId, data.id, mfg,
                                        friendlyName == null || friendlyName.isBlank() ? name : friendlyName,
                                        data.interfaceNumber, data.id, serialPort);
                                logger.debug("Parsed {}", usbSerialDeviceInformation);
                                result.add(usbSerialDeviceInformation);
                            }
                        }
                        intIdx++;
                    }
                    lastError = Native.getLastError();
                    if (lastError != WinError.ERROR_NO_MORE_ITEMS) {
                        logger.warn("Unexpected error while iterating USB device interfaces: {}",
                                Kernel32Util.formatMessage(lastError));
                    }
                    devIdx++;
                }
                lastError = Native.getLastError();
                if (lastError != WinError.ERROR_NO_MORE_ITEMS) {
                    logger.warn("Unexpected error while iterating USB devices: {}",
                            Kernel32Util.formatMessage(lastError));
                }
            } finally {
                apiInst.SetupDiDestroyDeviceInfoList(deviceInfoSet);
            }
        } else {
            lastError = Native.getLastError();
            logger.warn("Unable to enumerate USB devices: {}", Kernel32Util.formatMessage(lastError));
        }

        return result;
    }

    /**
     * Retrieves the specified device registry property using {@code SetupDiGetDeviceRegistryProperty} and returns
     * the result as a {@link String}. Might fail in an unpredictable way if the property value isn't a valid string.
     *
     * @param deviceInfoSet the handle to the {@code DeviceInfoSet} to read from.
     * @param property the code for the property to retrieve.
     * @param deviceInfoData the {@code DeviceInfoData} that identifies the element to retrieve the property from.
     * @return The resulting {@link String}.
     *
     * @throws Win32Exception If {@code SetupDiGetDeviceRegistryProperty} returns an unexpected status.
     */
    @Nullable
    protected String getDeviceRegistryPropertyString(HANDLE deviceInfoSet, int property,
            SP_DEVINFO_DATA deviceInfoData) {
        Memory buffer = getDeviceRegistryProperty(deviceInfoSet, property, deviceInfoData);
        return buffer == null ? null : buffer.getWideString(0L);
    }

    /**
     * Retrieves the specified device registry property using {@code SetupDiGetDeviceRegistryProperty} and returns
     * the result as a raw {@link Memory} buffer. The reason is that various properties can have different types/
     * data structures.
     *
     * @param deviceInfoSet the handle to the {@code DeviceInfoSet} to read from.
     * @param property the code for the property to retrieve.
     * @param deviceInfoData the {@code DeviceInfoData} that identifies the element to retrieve the property from.
     * @return The resulting {@link Memory} buffer.
     *
     * @throws Win32Exception If {@code SetupDiGetDeviceRegistryProperty} returns an unexpected status.
     */
    @Nullable
    protected Memory getDeviceRegistryProperty(HANDLE deviceInfoSet, int property, SP_DEVINFO_DATA deviceInfoData) {
        SetupApi apiInst = SetupApi.INSTANCE;
        IntByReference size = new IntByReference();
        int lastError;
        if (!apiInst.SetupDiGetDeviceRegistryProperty(deviceInfoSet, deviceInfoData, property, null, null, 0, size)
                && (lastError = Native.getLastError()) != WinError.ERROR_INSUFFICIENT_BUFFER) {
            if (lastError == WinError.ERROR_INVALID_DATA || lastError == ERROR_NO_SUCH_DEVINST) {
                return null;
            }
            throw new Win32Exception(lastError);
        }
        int sizeValue = size.getValue();
        if (sizeValue == 0) {
            return null;
        }
        Memory buffer = new Memory(sizeValue);
        if (!apiInst.SetupDiGetDeviceRegistryProperty(deviceInfoSet, deviceInfoData, property, null, buffer, sizeValue,
                null)) {
            lastError = Native.getLastError();
            if (lastError == WinError.ERROR_INVALID_DATA) {
                return null;
            }
            throw new Win32Exception(lastError);
        }
        return buffer;
    }

    /**
     * Retrieves the details for the specified device interface using {@code SetupDiGetDeviceInterfaceDetail} and
     * returns the result as a {@link List} of {@link String}s.
     *
     * @param deviceInfoSet the handle to the {@code DeviceInfoSet} to read from.
     * @param deviceInterfaceData the {@code DeviceInterfaceData} from which to retrieve the details.
     * @param deviceInfoData [out] the optional {@link SP_DEVINFO_DATA} structure that will be populated with
     *            {@code DeviceInfoData} about the device that supports the requested interface. The structure must
     *            first have been initialized with {@code DeviceInfoData.cbSize} to {@code sizeof(SP_DEVINFO_DATA)}.
     * @return The resulting {@link List} of {@link String}s.
     *
     * @throws Win32Exception If {@code SetupDiGetDeviceInterfaceDetail} returns an unexpected status.
     */
    protected List<String> getDeviceInterfaceDetails(HANDLE deviceInfoSet, SP_DEVICE_INTERFACE_DATA deviceInterfaceData,
            @Nullable SP_DEVINFO_DATA deviceInfoData) {
        SetupApi apiInst = SetupApi.INSTANCE;
        IntByReference size = new IntByReference();
        int lastError;
        if (!apiInst.SetupDiGetDeviceInterfaceDetail(deviceInfoSet, deviceInterfaceData, null, 0, size, deviceInfoData)
                && (lastError = Native.getLastError()) != WinError.ERROR_INSUFFICIENT_BUFFER) {
            if (lastError == WinError.ERROR_INVALID_DATA) {
                return List.of();
            }
            throw new Win32Exception(lastError);
        }
        int sizeValue = size.getValue();
        if (sizeValue == 0) {
            return List.of();
        }
        Memory result = new Memory(sizeValue);

        /*
         * The DWORD (uint) must contain the "size of the structure", which is only logical for those that
         * know how C compilers handle padding (64-bit pads where 32-bit doesn't).
         *
         * The 32-bit value represents: sizeOf(DWORD) + sizeOf(UTF16 char) = 4 + 2
         * The 64-bit value represents: sizeOf(DWORD) + sizeOf(UTF16 char) + padding = 4 + 2 + 2
         *
         * See https://stackoverflow.com/a/10729517 for further details.
         */
        result.setInt(0L, IS_64_BIT ? 8 : 6);
        if (!apiInst.SetupDiGetDeviceInterfaceDetail(deviceInfoSet, deviceInterfaceData, result, sizeValue, null,
                deviceInfoData)) {
            lastError = Native.getLastError();
            if (lastError == WinError.ERROR_INVALID_DATA) {
                return List.of();
            }
            throw new Win32Exception(lastError);
        }
        return readRegMultiSz(result, 4L);
    }

    /**
     * A {@code DevicePath} is a Windows concept that has a certain syntax. This method attempts to parse a USB
     * {@code DevicePath} and extract available data.
     *
     * @param devicePath the Windows USB {@code DevicePath} to parse.
     * @return The resulting {@link DevicePathData}.
     */
    @Nullable
    protected DevicePathData parseDevicePath(String devicePath) {
        Matcher m = devicePathPattern.matcher(devicePath.toLowerCase(Locale.ROOT));
        if (m.find()) {
            try {
                int vendorId = Integer.valueOf(m.group("vid"), 16);
                int productId = Integer.valueOf(m.group("pid"), 16);
                String s = m.group("mi");
                int interfaceNumber = s == null || s.isBlank() ? 0 : Integer.valueOf(s, 16);
                s = m.group("id");
                return new DevicePathData(vendorId, productId, s, interfaceNumber);
            } catch (NumberFormatException e) {
                logger.warn("Unable to parse USB device data idVendor: {}, idProduct {} or interface number {}: {}",
                        m.group("vid"), m.group("pid"), m.group("mi"), e.getMessage());
                return null;
            }
        }
        return null;
    }

    @Override
    public void startBackgroundScanning() {
        if (Platform.isWindows()) {
            boolean initScan = false;
            synchronized (this) {
                ScheduledFuture<?> scanTask = this.scanTask;
                WindowMessageHandler messageHandler = this.windowMessageHandler;
                if (windowMessageFailed) {
                    if (messageHandler != null) {
                        messageHandler.removeListener(this);
                        // Should not be necessary, but it doesn't hurt to make sure
                        messageHandler.terminate();
                        this.windowMessageHandler = null;
                    }
                    if (scanTask == null || scanTask.isDone()) {
                        this.scanTask = scheduler.scheduleWithFixedDelay(() -> {
                            doSingleScanInternal(false);
                        }, 0, scanInterval.toSeconds(), TimeUnit.SECONDS);
                    }
                } else {
                    if (scanTask != null) {
                        scanTask.cancel(true);
                        this.scanTask = null;
                    }
                    if (messageHandler == null) {
                        messageHandler = new WindowMessageHandler();
                        messageHandler.addListener(this);
                        this.windowMessageHandler = messageHandler;
                        scheduler.submit(messageHandler);
                        initScan = true;
                    }
                }
            }
            if (initScan) {
                doSingleScanInternal(false);
            }
        }
    }

    @Override
    public synchronized void stopBackgroundScanning() {
        WindowMessageHandler messageHandler = this.windowMessageHandler;
        if (messageHandler != null) {
            messageHandler.removeListener(this);
            messageHandler.terminate();
            this.windowMessageHandler = null;
        }
        ScheduledFuture<?> scanTask = this.scanTask;
        if (scanTask != null) {
            scanTask.cancel(true);
            this.scanTask = null;
        }
    }

    @Override
    public void deviceAdded(String devicePath) {
        logger.debug("New USB device discovered: {}", devicePath);
        doSingleScan();
    }

    @Override
    public void deviceRemoved(String devicePath) {
        logger.debug("USB device removed: {}", devicePath);
        doSingleScan();
    }

    @Override
    public void portAdded(String portName) {
        logger.debug("New serial port discovered: {}", portName);
    }

    @Override
    public void portRemoved(String portName) {
        logger.debug("Serial port removed: {}", portName);
    }

    @Override
    public void serviceTerminated() {
        logger.debug("Listening for window messages failed, falling back to interval scanning");
        windowMessageFailed = true;
        synchronized (this) {
            if (windowMessageHandler != null) {
                startBackgroundScanning();
            }
        }
    }

    /**
     * Parses a {@link Memory} buffer containing a {@code RegMultiSz} value into a list of strings. The result of using
     * this method on a buffer than doesn't contain a {@code RegMultiSz} value is unpredictable.
     *
     * @param buffer the buffer to parse.
     * @param offset the offset for where to start parsing.
     * @return The resulting {@link List} of {@link String}s.
     *
     * @throws IllegalArgumentException If the offset is invalid.
     */
    public static List<String> readRegMultiSz(Memory buffer, long offset) {
        long bufferSize = buffer.size();
        if (offset < 0L || offset >= bufferSize) {
            throw new IllegalArgumentException("Invalid offset " + offset + " for buffer of size " + bufferSize);
        }
        int size = (int) (bufferSize - offset) / 2;
        if (size == 0) {
            return List.of();
        }
        return readRegMultiSz(buffer.getCharArray(offset, size));
    }

    /**
     * Parses a char array containing a {@code RegMultiSz} value into a list of strings.
     *
     * @param chars the char array.
     * @return The resulting {@link List} of {@link String}s.
     */
    public static List<String> readRegMultiSz(char[] chars) {
        List<String> result = new ArrayList<>();
        int start = 0;
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] != 0) {
                continue;
            }
            if (start < i) {
                result.add(String.valueOf(chars, start, i - start));
            }
            start = i + 1;
        }
        return result;
    }
}
