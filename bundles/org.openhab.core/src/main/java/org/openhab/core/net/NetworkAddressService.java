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

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Interface that provides access to configured network addresses
 *
 * @author Stefan Triller - Initial contribution
 */
@NonNullByDefault
public interface NetworkAddressService {

    /**
     * Returns the user configured primary IPv4 address of the system
     *
     * @return IPv4 address as a String in format xxx.xxx.xxx.xxx or
     *         <code>null</code> if there is no interface or an error occurred
     */
    @Nullable
    String getPrimaryIpv4HostAddress();

    /**
     * Returns the user configured broadcast address, or the broadcast address of the user configured primary IPv4 if
     * not provided
     *
     * @return IPv4 broadcast address as a String in format xxx.xxx.xxx or
     *         <code>null</code> if no broadcast address is found or an error occurred
     */
    @Nullable
    String getConfiguredBroadcastAddress();

    /**
     * Use only one address per interface and family (IPv4 and IPv6). If set listeners should bind only to one address
     * per interface and family.
     *
     * @return use only one address per interface and family
     */
    boolean isUseOnlyOneAddress();

    /**
     * Use IPv6. If not set, IPv6 addresses should be completely ignored by listeners.
     *
     * @return use IPv6
     */
    boolean isUseIPv6();

    /**
     * Adds a {@link NetworkAddressChangeListener} that is notified about changes.
     *
     * @param listener The listener
     */
    public void addNetworkAddressChangeListener(NetworkAddressChangeListener listener);

    /**
     * Removes a {@link NetworkAddressChangeListener} so that it is no longer notified about changes.
     *
     * @param listener The listener
     */
    public void removeNetworkAddressChangeListener(NetworkAddressChangeListener listener);
}
