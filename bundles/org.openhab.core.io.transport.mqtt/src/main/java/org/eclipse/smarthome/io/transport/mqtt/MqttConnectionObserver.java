/**
 * Copyright (c) 2014,2019 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.io.transport.mqtt;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Implement this interface to get notified of connection state changes.
 * Register this observer at {@see MqttBrokerConnection}.
 *
 * @author David Graeff - Rewritten
 * @author Markus Rathgeb - Initial contribution and API
 */
@NonNullByDefault
public interface MqttConnectionObserver {
    /**
     * Inform the observer if a connection could be established or if a connection
     * is lost. This will be issued in the context of the Mqtt client thread and
     * requires that the control is returned quickly to not stall the Mqtt thread.
     *
     * @param state The new connection state
     * @param error An exception object (might be a MqttException) with the reason why
     *            a connection failed.
     */
    public void connectionStateChanged(MqttConnectionState state, @Nullable Throwable error);
}
