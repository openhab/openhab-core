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

import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * The CIDR (Class-less interdomain routing) notation is an IP address
 * and additionally ends with a slash followed by the network prefix length number.
 *
 * The toString() method will return a CIRDR representation, but the individual
 * address and prefix length can be accessed as well.
 *
 * Java has a class that exactly provides this {@link InterfaceAddress}, but unfortunately
 * no public constructor exists.
 *
 * @author David Graeff - Initial contribution
 */
@NonNullByDefault
public class CidrAddress {
    private final InetAddress address;
    private final int prefix;

    public CidrAddress(InetAddress address, short networkPrefixLength) {
        this.address = address;
        this.prefix = networkPrefixLength;
    }

    @Override
    public String toString() {
        if (prefix == 0) {
            return address.getHostAddress();
        } else {
            return address.getHostAddress() + "/" + prefix;
        }
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (!(o instanceof CidrAddress)) {
            return false;
        }
        CidrAddress c = (CidrAddress) o;
        return c.getAddress().equals(getAddress()) && c.getPrefix() == getPrefix();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getAddress().hashCode(), getPrefix());
    }

    public int getPrefix() {
        return prefix;
    }

    public InetAddress getAddress() {
        return address;
    }
}
