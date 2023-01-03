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
package org.openhab.core.config.discovery.usbserial.linuxsysfs.internal;

import static java.lang.Integer.toHexString;
import static java.nio.file.Files.*;
import static java.nio.file.attribute.PosixFilePermission.*;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.openhab.core.config.discovery.usbserial.linuxsysfs.internal.SysfsUsbSerialScanner.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openhab.core.config.discovery.usbserial.UsbSerialDeviceInformation;

/**
 * Unit tests for the {@link SysfsUsbSerialScanner}.
 *
 * @author Henning Sudbrock - Initial contribution
 */
@NonNullByDefault
public class SysFsUsbSerialScannerTest {

    public @TempDir @NonNullByDefault({}) File rootFolder;

    private static final String SYSFS_TTY_DEVICES_DIR = "sys/class/tty";
    private static final String DEV_DIR = "dev";
    private static final String SYSFS_USB_DEVICES_DIR = "sys/devices/pci0000:00/0000:00:14.0/usb1";

    private @NonNullByDefault({}) SysfsUsbSerialScanner scanner;

    private @NonNullByDefault({}) Path rootPath;
    private @NonNullByDefault({}) Path devPath;
    private @NonNullByDefault({}) Path sysfsTtyPath;
    private @NonNullByDefault({}) Path sysfsUsbPath;

    private int deviceIndexCounter = 0;

    @BeforeEach
    public void setup() throws IOException {
        // only run the tests on systems that support symbolic links
        assumeTrue(systemSupportsSymLinks());

        rootPath = rootFolder.toPath();

        devPath = rootPath.resolve(DEV_DIR);
        createDirectories(devPath);

        sysfsTtyPath = rootPath.resolve(SYSFS_TTY_DEVICES_DIR);
        createDirectories(sysfsTtyPath);

        sysfsUsbPath = rootPath.resolve(SYSFS_USB_DEVICES_DIR);
        createDirectories(sysfsUsbPath);

        scanner = new SysfsUsbSerialScanner();

        Map<String, Object> config = new HashMap<>();
        config.put(SYSFS_TTY_DEVICES_DIRECTORY_ATTRIBUTE, rootPath.resolve(SYSFS_TTY_DEVICES_DIR));
        config.put(DEV_DIRECTORY_ATTRIBUTE, rootPath.resolve(DEV_DIR));
        scanner.modified(config);
    }

    @Test
    public void testIOExceptionIfSysfsTtyDoesNotExist() throws IOException {
        delete(sysfsTtyPath);
        assertThrows(IOException.class, () -> scanner.scan());
    }

    @Test
    public void testNoResultsIfNoTtyDevicesExist() throws IOException {
        assertThat(scanner.scan(), is(empty()));
    }

    @Test
    public void testUsbSerialDevicesAreCorrectlyIdentified() throws IOException {
        createDevice("ttyUSB0", 0xABCD, 0X1234, "sample manufacturer", "sample product", "123-456-789", 0,
                "sample interface");
        createDevice("ttyUSB1", 0x0001, 0X0002, "another manufacturer", "product desc", "987-654-321", 1,
                "another interface");

        assertThat(scanner.scan(), hasSize(2));
        assertThat(scanner.scan(), hasItem(isUsbSerialDeviceInfo(0xABCD, 0x1234, "123-456-789", "sample manufacturer",
                "sample product", 0, "sample interface", rootPath.resolve(DEV_DIR).resolve("ttyUSB0").toString())));
        assertThat(scanner.scan(), hasItem(isUsbSerialDeviceInfo(0x0001, 0X0002, "987-654-321", "another manufacturer",
                "product desc", 1, "another interface", rootPath.resolve(DEV_DIR).resolve("ttyUSB1").toString())));
    }

    @Test
    public void testNonReadableDeviceFilesAreSkipped() throws IOException {
        createDevice("ttyUSB0", 0xABCD, 0X1234, "sample manufacturer", "sample product", "123-456-789", 0,
                "interfaceDesc");
        setPosixFilePermissions(devPath.resolve("ttyUSB0"), Set.of(OWNER_WRITE));
        assertThat(scanner.scan(), is(empty()));
    }

    @Test
    public void testNonWritableDeviceFilesAreSkipped() throws IOException {
        createDevice("ttyUSB0", 0xABCD, 0X1234, "sample manufacturer", "sample product", "123-456-789", 0,
                "interfaceDesc");
        setPosixFilePermissions(devPath.resolve("ttyUSB0"), Set.of(OWNER_READ));
        assertThat(scanner.scan(), is(empty()));
    }

    @Test
    public void testDeviceWithoutVendorIdIsSkipped() throws IOException {
        createDevice("ttyUSB0", 0xABCD, 0X1234, "sample manufacturer", "sample product", "123-456-789", 0,
                "interfaceDesc", DeviceCreationOption.NO_VENDOR_ID);
        assertThat(scanner.scan(), is(empty()));
    }

    @Test
    public void testDeviceWithoutProductIdIsSkipped() throws IOException {
        createDevice("ttyUSB0", 0xABCD, 0X1234, "sample manufacturer", "sample product", "123-456-789", 0,
                "interfaceDesc", DeviceCreationOption.NO_VENDOR_ID);
        assertThat(scanner.scan(), is(empty()));
    }

    @Test
    public void testDeviceWithoutInterfaceNumberIsSkipped() throws IOException {
        createDevice("ttyUSB0", 0xABCD, 0X1234, "sample manufacturer", "sample product", "123-456-789", 0,
                "interfaceDesc", DeviceCreationOption.NO_INTERFACE_NUMBER);
        assertThat(scanner.scan(), is(empty()));
    }

    @Test
    public void testNonUsbDeviceIsSkipped() throws IOException {
        createDevice("ttyUSB0", 0xABCD, 0X1234, "sample manufacturer", "sample product", "123-456-789", 0,
                "interfaceDesc", DeviceCreationOption.NON_USB_DEVICE);
        assertThat(scanner.scan(), is(empty()));
    }

    @Test
    public void testCanPerformScans() {
        // with given test setup, scans can be performed
        assertThat(scanner.canPerformScans(), is(true));
    }

    @Test
    public void testCannotPerformScansWithNonexistingSysDir() {
        Map<String, Object> config = new HashMap<>();
        config.put(SYSFS_TTY_DEVICES_DIRECTORY_ATTRIBUTE, rootPath.resolve("someNonexistingDir"));
        config.put(DEV_DIRECTORY_ATTRIBUTE, rootPath.resolve(DEV_DIR));
        scanner.modified(config);

        assertThat(scanner.canPerformScans(), is(false));
    }

    @Test
    public void testCannotPerformScansWithNonexistingDevDir() {
        Map<String, Object> config = new HashMap<>();
        config.put(SYSFS_TTY_DEVICES_DIRECTORY_ATTRIBUTE, rootPath.resolve(SYSFS_TTY_DEVICES_DIR));
        config.put(DEV_DIRECTORY_ATTRIBUTE, rootPath.resolve("someNonexistingDir"));
        scanner.modified(config);

        assertThat(scanner.canPerformScans(), is(false));
    }

    private void createDevice(String serialPortName, int vendorId, int productId, String manufacturer, String product,
            String serialNumber, int interfaceNumber, String interfaceDescription,
            DeviceCreationOption... deviceCreationOptions) throws IOException {
        int deviceIndex = deviceIndexCounter++;

        // Create the device file in /dev
        createFile(devPath.resolve(serialPortName));

        // Create the USB device folder structure
        Path usbDevicePath = sysfsUsbPath.resolve(String.format("1-%d", deviceIndex));
        Path usbInterfacePath = usbDevicePath.resolve(String.format("1-%d:1.%d", deviceIndex, interfaceNumber));
        Path serialDevicePath = usbInterfacePath.resolve(serialPortName);
        createDirectories(serialDevicePath);

        // Create the symlink into the USB device folder structure
        if (!List.of(deviceCreationOptions).contains(DeviceCreationOption.NON_USB_DEVICE)) {
            createSymbolicLink(sysfsTtyPath.resolve(serialPortName), serialDevicePath);
        } else {
            createSymbolicLink(sysfsTtyPath.resolve(serialPortName), devPath);
        }

        // Create the files containing information about the USB device
        if (!List.of(deviceCreationOptions).contains(DeviceCreationOption.NO_VENDOR_ID)) {
            write(createFile(usbDevicePath.resolve("idVendor")), toHexString(vendorId).getBytes());
        }
        if (!List.of(deviceCreationOptions).contains(DeviceCreationOption.NO_PRODUCT_ID)) {
            write(createFile(usbDevicePath.resolve("idProduct")), toHexString(productId).getBytes());
        }
        if (manufacturer != null) {
            write(createFile(usbDevicePath.resolve("manufacturer")), manufacturer.getBytes());
        }
        if (product != null) {
            write(createFile(usbDevicePath.resolve("product")), product.getBytes());
        }
        if (serialNumber != null) {
            write(createFile(usbDevicePath.resolve("serial")), serialNumber.getBytes());
        }

        // Create the files containing information about the USB interface
        if (!List.of(deviceCreationOptions).contains(DeviceCreationOption.NO_INTERFACE_NUMBER)) {
            write(createFile(usbInterfacePath.resolve("bInterfaceNumber")), toHexString(interfaceNumber).getBytes());
        }
        if (interfaceDescription != null) {
            write(createFile(usbInterfacePath.resolve("interface")), interfaceDescription.getBytes());
        }
    }

    private Matcher<UsbSerialDeviceInformation> isUsbSerialDeviceInfo(int vendorId, int productId, String serialNumber,
            String manufacturer, String product, int interfaceNumber, String interfaceDescription, String serialPort) {
        return equalTo(new UsbSerialDeviceInformation(vendorId, productId, serialNumber, manufacturer, product,
                interfaceNumber, interfaceDescription, serialPort));
    }

    private boolean systemSupportsSymLinks() throws IOException {
        try {
            createSymbolicLink(rootFolder.toPath().resolve("aSymbolicLink"), rootFolder.toPath());
            return true;
        } catch (UnsupportedOperationException e) {
            return false;
        }
    }

    private enum DeviceCreationOption {
        NO_VENDOR_ID,
        NO_PRODUCT_ID,
        NO_INTERFACE_NUMBER,
        NON_USB_DEVICE;
    }
}
