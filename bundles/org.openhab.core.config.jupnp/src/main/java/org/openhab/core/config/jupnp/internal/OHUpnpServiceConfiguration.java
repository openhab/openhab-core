/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
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
package org.openhab.core.config.jupnp.internal;

import java.util.concurrent.ExecutorService;

import org.jupnp.OSGiUpnpServiceConfiguration;
import org.jupnp.QueueingThreadPoolExecutor;
import org.jupnp.UpnpServiceConfiguration;
import org.openhab.basefixes.util.concurrent.LinkedTransferQueue;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;

/**
 * Uses the Java 11 {@link LinkedTransferQueue} with jUPnP as workaround for the buggy OpenJDK 17 implementation.
 *
 * @see <a href="https://bugs.openjdk.org/browse/JDK-8301341">JDK-8301341: LinkedTransferQueue does not respect timeout
 *      for poll()</a>
 * @see <a href="https://github.com/openhab/openhab-core/issues/3755">openhab-core#3755: LinkedTransferQueue in OpenJDK
 *      17 sometimes causes high CPU usage</a>
 *
 * @author Wouter Born - Initial contribution
 */
@Component(configurationPid = "org.jupnp", configurationPolicy = ConfigurationPolicy.REQUIRE, service = UpnpServiceConfiguration.class)
public class OHUpnpServiceConfiguration extends OSGiUpnpServiceConfiguration {
    @Override
    protected ExecutorService createMainExecutorService() {
        return QueueingThreadPoolExecutor.createInstance("upnp-main", threadPoolSize, new LinkedTransferQueue<>());
    }

    @Override
    protected ExecutorService createAsyncProtocolExecutorService() {
        return QueueingThreadPoolExecutor.createInstance("upnp-async", asyncThreadPoolSize,
                new LinkedTransferQueue<>());
    }

    @Override
    protected ExecutorService createRemoteProtocolExecutorService() {
        return QueueingThreadPoolExecutor.createInstance("upnp-remote", remoteThreadPoolSize,
                new LinkedTransferQueue<>());
    }
}
