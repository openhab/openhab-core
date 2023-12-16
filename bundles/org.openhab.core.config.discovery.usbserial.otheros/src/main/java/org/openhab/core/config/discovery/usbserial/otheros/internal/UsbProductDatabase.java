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

import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * This is a database map between hex 'vendorId:productId' pairs and the descriptions of known products. This is
 * required because in some cases the USB scanners fail to read the product description strings, so this provides
 * fallback descriptions.
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
@NonNullByDefault
public class UsbProductDatabase {

    public static final Map<String, String> PRODUCT_NAMES = Map.of(
    // @formatter:off
            "0403:6001", "FTDI / UART", // this won't match for Z-Wave or Zigbee
            "0403:8A28", "FTDI / ZigBee",
            "0451:16A8", "Texas Instruments / ZigBee",
            "0658:0200", "Aeotec / Z-Wave",
            "10C4:89FB", "Silicon Laboratories / ZigBee",
            "1CF1:0030", "dresden elektronik / ZigBee"
    // @formatter:on
    );

    public static @Nullable String getProduct(int vendorId, int productId) {
        return PRODUCT_NAMES.get(String.format("%04x:%04x", vendorId, productId));
    }
}
