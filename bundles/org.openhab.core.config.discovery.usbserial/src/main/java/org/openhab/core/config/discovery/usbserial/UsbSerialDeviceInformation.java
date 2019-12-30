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
package org.openhab.core.config.discovery.usbserial;

import static java.util.Objects.requireNonNull;

import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * This is a data container for information about a USB device and the serial port that can be
 * used to access the device using a serial interface.
 * <p/>
 * It contains, on the one hand, information from the USB standard device descriptor and standard interface descriptor,
 * and, on
 * the other hand, the name of the serial port (for Linux, this would be, e.g., '/dev/ttyUSB0', for Windows, e.g.,
 * 'COM4').
 *
 * @author Henning Sudbrock - Initial contribution
 */
@NonNullByDefault
public class UsbSerialDeviceInformation {

    private final int vendorId;
    private final int productId;

    private final @Nullable String serialNumber;
    private final @Nullable String manufacturer;
    private final @Nullable String product;

    private final int interfaceNumber;
    private final @Nullable String interfaceDescription;

    private final String serialPort;

    public UsbSerialDeviceInformation(int vendorId, int productId, @Nullable String serialNumber,
            @Nullable String manufacturer, @Nullable String product, int interfaceNumber,
            @Nullable String interfaceDescription, String serialPort) {
        this.vendorId = requireNonNull(vendorId);
        this.productId = requireNonNull(productId);

        this.serialNumber = serialNumber;
        this.manufacturer = manufacturer;
        this.product = product;

        this.interfaceNumber = interfaceNumber;
        this.interfaceDescription = interfaceDescription;

        this.serialPort = requireNonNull(serialPort);
    }

    /**
     * @return The vendor ID of the USB device (field 'idVendor' in the USB standard device descriptor).
     */
    public int getVendorId() {
        return vendorId;
    }

    /**
     * @return The product ID of the USB device (field 'idProduct' in the USB standard device descriptor).
     */
    public int getProductId() {
        return productId;
    }

    /**
     * @return The serial number of the USB device (field 'iSerialNumber' in the USB standard device descriptor).
     */
    public @Nullable String getSerialNumber() {
        return serialNumber;
    }

    /**
     * @return The manufacturer of the USB device (field 'iManufacturer' in the USB standard device descriptor).
     */
    public @Nullable String getManufacturer() {
        return manufacturer;
    }

    /**
     * @return The product description of the USB device (field 'iProduct' in the USB standard device descriptor).
     */
    public @Nullable String getProduct() {
        return product;
    }

    /**
     * @return The interface number of the used USB interface (field 'bInterfaceNumber' in the USB standard interface
     *         descriptor).
     */
    public int getInterfaceNumber() {
        return interfaceNumber;
    }

    /**
     * @return Description of the used USB interface (field 'iInterface' in the USB standard interface descriptor).
     */
    public @Nullable String getInterfaceDescription() {
        return interfaceDescription;
    }

    /**
     * @return The name of the serial port assigned to the USB device. Examples: /dev/ttyUSB1, COM4
     */
    public String getSerialPort() {
        return serialPort;
    }

    @SuppressWarnings("null")
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + vendorId;
        result = prime * result + productId;
        result = prime * result + interfaceNumber;
        result = prime * result + serialPort.hashCode();
        result = prime * result + ((manufacturer == null) ? 0 : manufacturer.hashCode());
        result = prime * result + ((product == null) ? 0 : product.hashCode());
        result = prime * result + ((serialNumber == null) ? 0 : serialNumber.hashCode());
        result = prime * result + ((interfaceDescription == null) ? 0 : interfaceDescription.hashCode());
        return result;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }

        UsbSerialDeviceInformation other = (UsbSerialDeviceInformation) obj;

        if (vendorId != other.vendorId) {
            return false;
        }

        if (productId != other.productId) {
            return false;
        }

        if (interfaceNumber != other.interfaceNumber) {
            return false;
        }

        if (!serialPort.equals(other.serialPort)) {
            return false;
        }

        if (!Objects.equals(manufacturer, other.manufacturer)) {
            return false;
        }

        if (!Objects.equals(product, other.product)) {
            return false;
        }

        if (!Objects.equals(serialNumber, other.serialNumber)) {
            return false;
        }

        if (!Objects.equals(interfaceDescription, other.interfaceDescription)) {
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        return String.format(
                "UsbSerialDeviceInformation [vendorId=0x%04X, productId=0x%04X, serialNumber=%s, manufacturer=%s, "
                        + "product=%s, interfaceNumber=0x%02X, interfaceDescription=%s, serialPort=%s]",
                vendorId, productId, serialNumber, manufacturer, product, interfaceNumber, interfaceDescription,
                serialPort);
    }

}
