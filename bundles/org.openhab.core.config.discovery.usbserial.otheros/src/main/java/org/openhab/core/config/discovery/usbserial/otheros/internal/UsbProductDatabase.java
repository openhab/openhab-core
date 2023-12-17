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
 * <p>
 * It contains a map of vendor ids, product ids, and product descriptions. The descriptions should include at least a
 * tag indicating the type of binding it supports. The entries MUST be formatted as follows:
 * <p>
 * <li>keys e.g. "vendorId:productId" -- each part comprising four <b>UPPER</b>-case hex characters.
 * <li>entries e.g. "vendor OEM / sub-vendor name / ZigBee or Z-Wave" -- including a supported binding id.
 * <p>
 * Note: supported binding ids can be as follows:
 * <li>'Zigbee'
 * <li>'Z-Wave'
 * <li>'EnOcean'
 * Please do NOT include supported binding id in the case of very commonly used generic serial chips, since this may
 * lead to false-positive discoveries.
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
@NonNullByDefault
public class UsbProductDatabase {

    // @formatter:off
    public static final Map<String, String> PRODUCT_NAMES = Map.of(
        /*
         * ==== EnOcean sticks ====
         */
        // the following generic is used in EnOcean sticks => EXCLUDE supported binding ids!
        "0403:6001", "Future Technology Devices / GENERIC",

        /*
         * ==== Zigbee sticks ====
         */
        "0403:8A28", "Future Technology Devices / Rainforest Automation / ZigBee",
        "0451:16A8", "Texas Instruments / ZigBee",
        "10C4:89FB", "Silicon Laboratories / ZigBee",
        "1CF1:0030", "dresden elektronik / ZigBee",

        /*
         * ==== Z-Wave sticks ====
         */
        "0658:0200", "Sigma Designs / Aeotec / ZWave.me UZB / Z-Wave",
        "1A86:55D4", "Nanjing Qinheng Microelectronics / Zooz 800 / Z-Wave",

        /*
         * ==== Zigbee sticks || Z-Wave sticks ====
         */
        // maybe the following is generic => please report false positives !!
        "10C4:EA60", "Silicon Laboratories / Aeon Labs / Zooz 700 / sonoff / Z-Wave / Zigbee"
    );
    // @formatter:on

    public static @Nullable String getProduct(int vendorId, int productId) {
        return PRODUCT_NAMES.get(String.format("%04x:%04x", vendorId, productId));
    }
}
