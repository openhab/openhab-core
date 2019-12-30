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

import static java.nio.file.Files.*;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.discovery.usbserial.UsbSerialDeviceInformation;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link UsbSerialScanner} that scans the system for USB devices which provide a serial port by inspecting the
 * so-called 'sysfs' (see also https://en.wikipedia.org/wiki/Sysfs) provided by Linux (a pseudo file system provided by
 * the Linux kernel usually mounted at '/sys').
 * <p/>
 * A scan starts by inspecting the contents of the directory '/sys/class/tty'. This directory contains a symbolic link
 * for every serial port style device that points to the device information provided by the sysfs in some subdirectory
 * of '/sys/devices'.
 * <p/>
 * The scan considers only those serial ports for which the corresponding device file (in folder '/dev'; e.g.:
 * '/dev/ttyUSB0') is both readable and writable, as otherwise the serial port cannot be used by any binding. For those
 * serial ports, the scan checks whether the serial port actually originates from a USB device, by inspecting the
 * information provided by the sysfs in the folder pointed to by the symbolic link.
 * <p/>
 * If the device providing the serial port is a USB device, information about the device (vendor ID, product ID, etc.)
 * is collected from the sysfs and returned together with the name of the serial port in form of a
 * {@link UsbSerialDeviceInformation}.
 *
 * @author Henning Sudbrock - Initial contribution
 */
@Component(configurationPid = "discovery.usbserial.linuxsysfs.usbserialscanner")
@NonNullByDefault
public class SysfsUsbSerialScanner implements UsbSerialScanner {

    private final Logger logger = LoggerFactory.getLogger(SysfsUsbSerialScanner.class);

    public static final String SYSFS_TTY_DEVICES_DIRECTORY_ATTRIBUTE = "sysfsTtyDevicesPath";
    public static final String DEV_DIRECTORY_ATTRIBUTE = "devPath";

    private static final String SYSFS_TTY_DEVICES_DIRECTORY_DEFAULT = "/sys/class/tty";
    private static final String DEV_DIRECTORY_DEFAULT = "/dev";

    private String sysfsTtyDevicesDirectory = SYSFS_TTY_DEVICES_DIRECTORY_DEFAULT;
    private String devDirectory = DEV_DIRECTORY_DEFAULT;

    private static final String SYSFS_FILENAME_USB_VENDOR_ID = "idVendor";
    private static final String SYSFS_FILENAME_USB_PRODUCT_ID = "idProduct";
    private static final String SYSFS_FILENAME_USB_SERIAL_NUMBER = "serial";
    private static final String SYSFS_FILENAME_USB_MANUFACTURER = "manufacturer";
    private static final String SYSFS_FILENAME_USB_PRODUCT = "product";
    private static final String SYSFS_FILENAME_USB_INTERFACE_NUMBER = "bInterfaceNumber";
    private static final String SYSFS_FILENAME_USB_INTERFACE = "interface";

    /**
     * In the sysfs, directories for USB interfaces have the following format (cf., e.g.,
     * http://www.linux-usb.org/FAQ.html#i6), where there can be one or more USB port numbers separated by dots.
     *
     * <pre>
     * {@code <#bus>-<#port>.<#port>:<#config>:<#interface>}
     * </pre>
     *
     * Example: {@code 3-1.3:1.0}
     * <p/>
     * This format is captured by this {@link Pattern}.
     */
    private static final Pattern SYSFS_USB_INTERFACE_DIRECTORY_PATTERN = Pattern
            .compile("\\d+-(\\d+\\.?)*\\d+:\\d+\\.\\d+");

    @Activate
    protected void activate(Map<String, Object> config) {
        extractConfiguration(config);
    }

    @Modified
    protected void modified(Map<String, Object> config) {
        extractConfiguration(config);
    }

    @Override
    public Set<UsbSerialDeviceInformation> scan() throws IOException {
        Set<UsbSerialDeviceInformation> result = new HashSet<>();

        for (SerialPortInfo serialPortInfo : getSerialPortInfos()) {
            try {
                UsbSerialDeviceInformation usbSerialDeviceInfo = tryGetUsbSerialDeviceInformation(serialPortInfo);
                if (usbSerialDeviceInfo != null) {
                    result.add(usbSerialDeviceInfo);
                }
            } catch (IOException e) {
                logger.warn("Could not extract USB device information for serial port {}: {}", serialPortInfo,
                        e.getMessage());
            }
        }

        return result;
    }

    @Override
    public boolean canPerformScans() {
        return isReadable(Paths.get(sysfsTtyDevicesDirectory)) && isReadable(Paths.get(devDirectory));
    }

    /**
     * Gets the set of all found serial ports, by searching through the tty devices directory in the sysfs and
     * checking for each found serial port if the device file in the devices folder is both readable and writable.
     *
     * @throws IOException If there is a problem reading files from the sysfs tty devices directory.
     */
    private Set<SerialPortInfo> getSerialPortInfos() throws IOException {
        Set<SerialPortInfo> result = new HashSet<>();

        try (DirectoryStream<Path> sysfsTtyPaths = newDirectoryStream(Paths.get(sysfsTtyDevicesDirectory))) {
            for (Path sysfsTtyPath : sysfsTtyPaths) {
                String serialPortName = sysfsTtyPath.getFileName().toString();
                Path devicePath = Paths.get(devDirectory).resolve(serialPortName);
                Path sysfsDevicePath = getSysfsDevicePath(sysfsTtyPath);
                if (sysfsDevicePath != null && isReadable(devicePath) && isWritable(devicePath)) {
                    result.add(new SerialPortInfo(devicePath, sysfsDevicePath));
                }
            }
        }

        return result;
    }

    /**
     * In the sysfs, the directory 'class/tty' contains a symbolic link for every serial port style device, i.e., also
     * for serial devices. This symbolic link points to the directory for that device within the sysfs device tree. This
     * method returns the directory to which this symbolic link points for a given serial port.
     * <p/>
     * If the symbolic link cannot be converted to the real path, null is returned and a warning is logged.
     */
    private @Nullable Path getSysfsDevicePath(Path ttyFile) {
        try {
            return ttyFile.toRealPath();
        } catch (IOException e) {
            logger.warn("Could not find the device path for {} in the sysfs: {}", ttyFile, e.getMessage());
            return null;
        }
    }

    /**
     * Checks whether the provided device path in sysfs points to a folder within the sysfs description of a USB device;
     * if so, extracts the USB device information from sysfs and constructs a {@link UsbSerialDeviceInformation} using
     * the {@link SerialPortInfo} and the information about the USB device gathered from sysfs.
     * <p/>
     * Returns null if the path does not point to a folder within the sysfs description of a USB device.
     */
    private @Nullable UsbSerialDeviceInformation tryGetUsbSerialDeviceInformation(SerialPortInfo serialPortInfo)
            throws IOException {
        Path usbInterfacePath = getUsbInterfaceParentPath(serialPortInfo.getSysfsPath());

        if (usbInterfacePath == null) {
            return null;
        }

        Path usbDevicePath = usbInterfacePath.getParent();
        if (isUsbDevicePath(usbDevicePath)) {
            return createUsbSerialDeviceInformation(usbDevicePath, usbInterfacePath,
                    serialPortInfo.getDevicePath().toString());
        } else {
            return null;
        }
    }

    /**
     * Walks up the directory structure of a path in the sysfs, trying to find a directory that represents an interface
     * of a USB device.
     */
    private @Nullable Path getUsbInterfaceParentPath(Path sysfsPath) {
        if (sysfsPath.getFileName() == null) {
            return null;
        } else if (SYSFS_USB_INTERFACE_DIRECTORY_PATTERN.matcher(sysfsPath.getFileName().toString()).matches()) {
            return sysfsPath;
        } else {
            Path parentPath = sysfsPath.getParent();
            if (parentPath == null) {
                return null;
            } else {
                return getUsbInterfaceParentPath(parentPath);
            }
        }
    }

    private boolean isUsbDevicePath(Path path) {
        return containsFile(path, SYSFS_FILENAME_USB_PRODUCT_ID) && containsFile(path, SYSFS_FILENAME_USB_VENDOR_ID);
    }

    /**
     * Constructs a {@link UsbSerialDeviceInformation} from a serial port and the information found in the sysfs about a
     * USB device.
     */
    private UsbSerialDeviceInformation createUsbSerialDeviceInformation(Path usbDevicePath, Path usbInterfacePath,
            String serialPortName) throws IOException {
        int vendorId = Integer.parseInt(getContent(usbDevicePath.resolve(SYSFS_FILENAME_USB_VENDOR_ID)), 16);
        int productId = Integer.parseInt(getContent(usbDevicePath.resolve(SYSFS_FILENAME_USB_PRODUCT_ID)), 16);

        String serialNumber = getContentIfFileExists(usbDevicePath.resolve(SYSFS_FILENAME_USB_SERIAL_NUMBER));
        String manufacturer = getContentIfFileExists(usbDevicePath.resolve(SYSFS_FILENAME_USB_MANUFACTURER));
        String product = getContentIfFileExists(usbDevicePath.resolve(SYSFS_FILENAME_USB_PRODUCT));

        int interfaceNumber = Integer
                .parseInt(getContent(usbInterfacePath.resolve(SYSFS_FILENAME_USB_INTERFACE_NUMBER)), 16);
        String interfaceDescription = getContentIfFileExists(usbInterfacePath.resolve(SYSFS_FILENAME_USB_INTERFACE));

        return new UsbSerialDeviceInformation(vendorId, productId, serialNumber, manufacturer, product, interfaceNumber,
                interfaceDescription, serialPortName);
    }

    private boolean containsFile(Path directoryPath, String filename) {
        Path filePath = directoryPath.resolve(filename);
        return exists(filePath) && !isDirectory(filePath);
    }

    private String getContent(Path path) throws IOException {
        return new String(readAllBytes(path)).trim();
    }

    private @Nullable String getContentIfFileExists(Path path) throws IOException {
        return exists(path) ? getContent(path) : null;
    }

    private void extractConfiguration(Map<String, Object> config) {
        String newSysfsTtyDevicesDirectory = config
                .getOrDefault(SYSFS_TTY_DEVICES_DIRECTORY_ATTRIBUTE, SYSFS_TTY_DEVICES_DIRECTORY_DEFAULT).toString();
        String newDevDirectory = config.getOrDefault(DEV_DIRECTORY_ATTRIBUTE, DEV_DIRECTORY_DEFAULT).toString();

        boolean configurationIsChanged = !(Objects.equals(sysfsTtyDevicesDirectory, newSysfsTtyDevicesDirectory)
                && Objects.equals(devDirectory, newDevDirectory));

        if (configurationIsChanged) {
            sysfsTtyDevicesDirectory = newSysfsTtyDevicesDirectory;
            devDirectory = newDevDirectory;
        }

        if (!canPerformScans()) {
            String logString = String.format(
                    "Cannot perform scans with this configuration: sysfsTtyDevicesDirectory: {}, devDirectory: {}",
                    sysfsTtyDevicesDirectory, devDirectory);

            if (configurationIsChanged) {
                // Warn if the configuration was actively changed
                logger.warn(logString);
            } else {
                // Otherwise, only debug log - so that, in particular, on Non-Linux systems users do not see warning
                logger.debug(logString);
            }
        }
    }

    private static class SerialPortInfo {
        private final Path devicePath;
        private final Path sysfsPath;

        public SerialPortInfo(Path devicePath, Path sysfsPath) {
            this.devicePath = devicePath;
            this.sysfsPath = sysfsPath;
        }

        public Path getDevicePath() {
            return devicePath;
        }

        public Path getSysfsPath() {
            return sysfsPath;
        }
    }

}
