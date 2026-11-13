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
package org.openhab.core.net;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.openhab.core.internal.common.SafeCallerImpl;

/**
 * Tests for NetUtil class
 *
 * @author Stefan Triller - Initial contribution
 * @author Mark Herwege - added tests for broadcast address
 */
@NonNullByDefault
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

    @Test
    public void checkValidRangeCountAndSort() throws UnknownHostException {
        InetAddress testableAddress = InetAddress.getByName("192.168.1.4");
        List<InetAddress> addresses = NetUtil
                .getAddressesRangeByCidrAddress(new CidrAddress(testableAddress, (short) 24), 24);

        assertEquals(254, addresses.size());
        assertEquals("192.168.1.1", addresses.getFirst().getHostAddress());
        assertEquals("192.168.1.254", addresses.get(253).getHostAddress());
    }

    @Test
    public void checkValidLargeRangeCountAndSort() throws UnknownHostException {
        InetAddress testableAddress = InetAddress.getByName("127.0.1.12");
        List<InetAddress> addresses = NetUtil
                .getAddressesRangeByCidrAddress(new CidrAddress(testableAddress, (short) 16), 16);

        assertEquals(65534, addresses.size());
        assertEquals("127.0.0.1", addresses.getFirst().getHostAddress());
        assertEquals("127.0.255.254", addresses.get(65533).getHostAddress());
    }

    @Test
    public void checkNotAllowedPrefixLength() throws UnknownHostException {
        InetAddress testableAddress = InetAddress.getByName("192.168.1.0");
        List<InetAddress> addresses = NetUtil
                .getAddressesRangeByCidrAddress(new CidrAddress(testableAddress, (short) 16), 24);

        assertEquals(0, addresses.size());
    }

    @Test
    public void checkValidRangeForPrefixNotAlignedToByte() throws UnknownHostException {
        InetAddress testableAddress = InetAddress.getByName("192.168.5.10");
        List<InetAddress> addresses = NetUtil
                .getAddressesRangeByCidrAddress(new CidrAddress(testableAddress, (short) 22), 22);

        assertEquals(1022, addresses.size());
        assertEquals("192.168.4.1", addresses.getFirst().getHostAddress());
        assertEquals("192.168.7.254", addresses.getLast().getHostAddress());
    }

    @Test
    public void checkValidRangeForSmallSubnet() throws UnknownHostException {
        InetAddress testableAddress = InetAddress.getByName("10.0.0.6");
        List<InetAddress> addresses = NetUtil
                .getAddressesRangeByCidrAddress(new CidrAddress(testableAddress, (short) 30), 30);

        assertEquals(2, addresses.size());
        assertEquals("10.0.0.5", addresses.getFirst().getHostAddress());
        assertEquals("10.0.0.6", addresses.getLast().getHostAddress());
    }

    @Test
    public void fullRangeOfAddressesToScanCoversAllIPv4InterfaceAddresses() throws UnknownHostException {
        List<CidrAddress> interfaceAddresses = List.of(
                new CidrAddress(InetAddress.getByName("192.168.1.4"), (short) 24),
                new CidrAddress(InetAddress.getByName("10.0.0.5"), (short) 30),
                new CidrAddress(InetAddress.getByName("fe80::1"), (short) 64));

        try (MockedStatic<NetUtil> netUtil = mockStatic(NetUtil.class, CALLS_REAL_METHODS)) {
            netUtil.when(NetUtil::getAllInterfaceAddresses).thenReturn(interfaceAddresses);

            List<InetAddress> addresses = NetUtil.getFullRangeOfAddressesToScan();

            assertEquals(254 + 2, addresses.size());
            assertTrue(addresses.stream().allMatch(Inet4Address.class::isInstance));
            assertEquals("192.168.1.1", addresses.getFirst().getHostAddress());
            assertEquals("10.0.0.5", addresses.get(254).getHostAddress());
            assertEquals("10.0.0.6", addresses.getLast().getHostAddress());
        }
    }

    @Test
    public void primaryAndBroadcastAddressResolvedOnceAnIpv4AddressAppears() {
        SafeCallerImpl safeCaller = new SafeCallerImpl(null);
        LateAddressNetUtil netUtil = new LateAddressNetUtil(safeCaller);
        NetworkAddressChangeListener listener = mock(NetworkAddressChangeListener.class);
        netUtil.addNetworkAddressChangeListener(listener);
        doAnswer(invocation -> {
            netUtil.broadcastSeenByListener = netUtil.getConfiguredBroadcastAddress();
            return null;
        }).when(listener).onPrimaryAddressChanged(any(), any());
        try {
            netUtil.activate(Map.of());
            assertNull(netUtil.getPrimaryIpv4HostAddress());
            assertNull(netUtil.getConfiguredBroadcastAddress());

            netUtil.localAddress = "192.168.1.18";
            verify(listener, timeout(5000)).onPrimaryAddressChanged(null, "192.168.1.18");
            assertEquals("192.168.1.18", netUtil.getPrimaryIpv4HostAddress());
            assertEquals("192.168.1.255", netUtil.getConfiguredBroadcastAddress());
            assertEquals("192.168.1.255", netUtil.broadcastSeenByListener);
        } finally {
            netUtil.deactivate();
            safeCaller.deactivate();
        }
    }

    @Test
    public void configuredBroadcastAddressKeptWhenPrimaryAddressAppears() {
        SafeCallerImpl safeCaller = new SafeCallerImpl(null);
        LateAddressNetUtil netUtil = new LateAddressNetUtil(safeCaller);
        NetworkAddressChangeListener listener = mock(NetworkAddressChangeListener.class);
        netUtil.addNetworkAddressChangeListener(listener);
        try {
            netUtil.activate(Map.of("broadcastAddress", "10.0.0.255"));

            netUtil.localAddress = "192.168.1.18";
            verify(listener, timeout(5000)).onPrimaryAddressChanged(null, "192.168.1.18");
            assertEquals("10.0.0.255", netUtil.getConfiguredBroadcastAddress());
        } finally {
            netUtil.deactivate();
            safeCaller.deactivate();
        }
    }

    private static class LateAddressNetUtil extends NetUtil {
        volatile @Nullable String localAddress;
        volatile @Nullable String broadcastSeenByListener;

        LateAddressNetUtil(SafeCallerImpl safeCaller) {
            super(safeCaller);
        }

        @Override
        @Nullable
        String getFirstLocalIPv4Address() {
            return localAddress;
        }

        @Override
        @Nullable
        String getPrimaryBroadcastAddress() {
            return localAddress == null ? null : "192.168.1.255";
        }
    }
}
