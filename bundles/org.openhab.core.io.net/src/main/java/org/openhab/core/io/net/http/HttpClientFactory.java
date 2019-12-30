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
import org.eclipse.jetty.client.HttpClient;

/**
 * Factory class to create Jetty http clients
 *
 * @author Michael Bock - Initial contribution
 * @author Martin van Wingerden - add createHttpClient without endpoint
 */
@NonNullByDefault
public interface HttpClientFactory {

    /**
     * Creates a new Jetty http client.
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
    HttpClient createHttpClient(String consumerName, String endpoint);

    /**
     * Creates a new Jetty http client.
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
    HttpClient createHttpClient(String consumerName);

    /**
     * Returns the shared Jetty http client. You must not call any setter methods or {@code stop()} on it.
     * The returned client is already started.
     *
     * @return the shared Jetty http client
     */
    HttpClient getCommonHttpClient();
}
