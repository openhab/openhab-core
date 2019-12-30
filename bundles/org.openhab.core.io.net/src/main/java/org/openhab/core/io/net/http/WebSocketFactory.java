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
package org.openhab.core.io.net.http;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jetty.websocket.client.WebSocketClient;

/**
 * Factory class to create Jetty web socket clients
 *
 * @author Kai Kreuzer - Initial contribution
 */
@NonNullByDefault
public interface WebSocketFactory {

    /**
     * Creates a new Jetty web socket client.
     * The returned client is not started yet. You have to start it yourself before using.
     * Don't forget to stop a started client again after its usage.
     * The client lifecycle should be the same as for your service.
     * DO NOT CREATE NEW CLIENTS FOR EACH REQUEST!
     *
     * @param consumerName the for identifying the consumer in the Jetty thread pool.
     *            Must be between 4 and 20 characters long and must contain only the following characters [a-zA-Z0-9-_]
     * @param endpoint the desired endpoint, protocol and host are sufficient
     * @return the Jetty client
     * @throws NullPointerException if {@code endpoint} or {@code consumerName} is {@code null}
     * @throws IllegalArgumentException if {@code consumerName} is invalid
     */
    @Deprecated
    WebSocketClient createWebSocketClient(String consumerName, String endpoint);

    /**
     * Creates a new Jetty web socket client.
     * The returned client is not started yet. You have to start it yourself before using.
     * Don't forget to stop a started client again after its usage.
     * The client lifecycle should be the same as for your service.
     * DO NOT CREATE NEW CLIENTS FOR EACH REQUEST!
     *
     * @param consumerName the for identifying the consumer in the Jetty thread pool.
     *            Must be between 4 and 20 characters long and must contain only the following characters [a-zA-Z0-9-_]
     * @return the Jetty client
     * @throws NullPointerException if {@code consumerName} is {@code null}
     * @throws IllegalArgumentException if {@code consumerName} is invalid
     */
    WebSocketClient createWebSocketClient(String consumerName);

    /**
     * Returns a shared Jetty web socket client. You must not call any setter methods or {@code stop()} on it.
     * The returned client is already started.
     *
     * @return a shared Jetty web socket client
     */
    WebSocketClient getCommonWebSocketClient();
}
