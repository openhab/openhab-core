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
package org.openhab.core.io.transport.modbus.endpoint;

import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Class representing pooling related configuration of a single endpoint
 *
 * This class implements equals hashcode constract, and thus is suitable for use as keys in HashMaps, for example.
 *
 * @author Sami Salonen - Initial contribution
 */
@NonNullByDefault
public class EndpointPoolConfiguration {

    /**
     * Delay between first TCP packet and successful TCP connection.
     */
    private long afterConnectionDelayMillis;

    /**
     * How long should be the minimum duration between previous transaction end and the next transaction with the same
     * endpoint.
     *
     * In milliseconds.
     */
    private long interTransactionDelayMillis;

    /**
     * How long should be the minimum duration between connection-establishments from the pool (with same endpoint). In
     * milliseconds.
     */
    private long interConnectDelayMillis;

    /**
     * How many times we want to try connecting to the endpoint before giving up. One means that connection
     * establishment is tried once.
     */
    private int connectMaxTries = 1;

    /**
     * Re-connect connection every X milliseconds. Negative means that connection is not disconnected automatically.
     * One can use 0ms to denote reconnection after every transaction (default).
     */
    private int reconnectAfterMillis;

    /**
     * How long before we give up establishing the connection. In milliseconds. Default of 0 means that system/OS
     * default is respected.
     */
    private int connectTimeoutMillis;

    public void setAfterConnectionDelayMillis(long afterConnectionDelayMillis) {
        this.afterConnectionDelayMillis = afterConnectionDelayMillis;
    }

    public long getAfterConnectionDelayMillis() {
        return afterConnectionDelayMillis;
    }

    public long getInterConnectDelayMillis() {
        return interConnectDelayMillis;
    }

    public void setInterConnectDelayMillis(long interConnectDelayMillis) {
        this.interConnectDelayMillis = interConnectDelayMillis;
    }

    public int getConnectMaxTries() {
        return connectMaxTries;
    }

    public void setConnectMaxTries(int connectMaxTries) {
        this.connectMaxTries = connectMaxTries;
    }

    public int getReconnectAfterMillis() {
        return reconnectAfterMillis;
    }

    public void setReconnectAfterMillis(int reconnectAfterMillis) {
        this.reconnectAfterMillis = reconnectAfterMillis;
    }

    public long getInterTransactionDelayMillis() {
        return interTransactionDelayMillis;
    }

    public void setInterTransactionDelayMillis(long interTransactionDelayMillis) {
        this.interTransactionDelayMillis = interTransactionDelayMillis;
    }

    public int getConnectTimeoutMillis() {
        return connectTimeoutMillis;
    }

    public void setConnectTimeoutMillis(int connectTimeoutMillis) {
        this.connectTimeoutMillis = connectTimeoutMillis;
    }

    @Override
    public int hashCode() {
        return Objects.hash(connectMaxTries, connectTimeoutMillis, interConnectDelayMillis, interTransactionDelayMillis,
                reconnectAfterMillis, afterConnectionDelayMillis);
    }

    @Override
    public String toString() {
        return "EndpointPoolConfiguration [interTransactionDelayMillis=" + interTransactionDelayMillis
                + ", interConnectDelayMillis=" + interConnectDelayMillis + ", connectMaxTries=" + connectMaxTries
                + ", reconnectAfterMillis=" + reconnectAfterMillis + ", connectTimeoutMillis=" + connectTimeoutMillis
                + ", afterConnectionDelayMillis=" + afterConnectionDelayMillis + "]";
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        EndpointPoolConfiguration rhs = (EndpointPoolConfiguration) obj;
        return connectMaxTries == rhs.connectMaxTries && connectTimeoutMillis == rhs.connectTimeoutMillis
                && interConnectDelayMillis == rhs.interConnectDelayMillis
                && interTransactionDelayMillis == rhs.interTransactionDelayMillis
                && reconnectAfterMillis == rhs.reconnectAfterMillis
                && afterConnectionDelayMillis == rhs.afterConnectionDelayMillis;
    }
}
