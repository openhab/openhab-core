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
package org.openhab.core.net;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

/**
 * Tests for NetUtil class
 *
 * @author Stefan Triller - Initial contribution
 * @author Mark Herwege - added tests for broadcast address
 */
public class NetUtilTest {

    @Test
    public void testNetwork() {
        String network = NetUtil.getIpv4NetAddress("192.168.0.1", (short) 24);
        assertThat(network, is("192.168.0.0"));

        network = NetUtil.getIpv4NetAddress("192.168.23.5", (short) 24);
        assertThat(network, is("192.168.23.0"));

        network = NetUtil.getIpv4NetAddress("172.16.42.23", (short) 16);
        assertThat(network, is("172.16.0.0"));

        network = NetUtil.getIpv4NetAddress("10.8.13.5", (short) 8);
        assertThat(network, is("10.0.0.0"));

        network = NetUtil.getIpv4NetAddress("192.168.5.8", (short) 23);
        assertThat(network, is("192.168.4.0"));

        network = NetUtil.getIpv4NetAddress("192.168.5.8", (short) 27);
        assertThat(network, is("192.168.5.0"));

        network = NetUtil.getIpv4NetAddress("192.168.5.8", (short) 29);
        assertThat(network, is("192.168.5.8"));

        try {
            network = NetUtil.getIpv4NetAddress("192.168.5.8", (short) 33);
        } catch (IllegalArgumentException iae) {
            assertThat(iae.getMessage(), is("Netmask '33' is out of bounds (1-32)"));
        }
        try {
            network = NetUtil.getIpv4NetAddress("192.168.58", (short) 24);
        } catch (IllegalArgumentException iae) {
            assertThat(iae.getMessage(), is("IP '192.168.58' is not a valid IPv4 address"));
        }
        try {
            network = NetUtil.getIpv4NetAddress("SOME_TEXT", (short) 24);
        } catch (IllegalArgumentException iae) {
            assertThat(iae.getMessage(), is("IP 'SOME_TEXT' is not a valid IPv4 address"));
        }
        try {
            network = NetUtil.getIpv4NetAddress("SOME_TEXT", (short) 42);
        } catch (IllegalArgumentException iae) {
            assertThat(iae.getMessage(), is("IP 'SOME_TEXT' is not a valid IPv4 address"));
        }
    }

    @Test
    public void testBroadcastAddress() {
        String broadcast = NetUtil.getIpv4NetBroadcastAddress("192.168.0.1", (short) 24);
        assertThat(broadcast, is("192.168.0.255"));

        broadcast = NetUtil.getIpv4NetBroadcastAddress("192.168.23.5", (short) 24);
        assertThat(broadcast, is("192.168.23.255"));

        broadcast = NetUtil.getIpv4NetBroadcastAddress("172.16.42.23", (short) 16);
        assertThat(broadcast, is("172.16.255.255"));

        broadcast = NetUtil.getIpv4NetBroadcastAddress("10.8.13.5", (short) 8);
        assertThat(broadcast, is("10.255.255.255"));

        broadcast = NetUtil.getIpv4NetBroadcastAddress("192.168.5.8", (short) 23);
        assertThat(broadcast, is("192.168.5.255"));

        broadcast = NetUtil.getIpv4NetBroadcastAddress("192.168.5.8", (short) 27);
        assertThat(broadcast, is("192.168.5.31"));

        broadcast = NetUtil.getIpv4NetBroadcastAddress("192.168.5.8", (short) 29);
        assertThat(broadcast, is("192.168.5.15"));

        try {
            broadcast = NetUtil.getIpv4NetBroadcastAddress("192.168.5.8", (short) 33);
        } catch (IllegalArgumentException iae) {
            assertThat(iae.getMessage(), is("Prefix '33' is out of bounds (1-32)"));
        }
        try {
            broadcast = NetUtil.getIpv4NetBroadcastAddress("192.168.58", (short) 24);
        } catch (IllegalArgumentException iae) {
            assertThat(iae.getMessage(), is("IP '192.168.58' is not a valid IPv4 address"));
        }
        try {
            broadcast = NetUtil.getIpv4NetBroadcastAddress("192.168.58", (short) 24);
        } catch (IllegalArgumentException iae) {
            assertThat(iae.getMessage(), is("IP '192.168.58' is not a valid IPv4 address"));
        }
        try {
            broadcast = NetUtil.getIpv4NetBroadcastAddress("SOME_TEXT", (short) 42);
        } catch (IllegalArgumentException iae) {
            assertThat(iae.getMessage(), is("IP 'SOME_TEXT' is not a valid IPv4 address"));
        }
    }
}
