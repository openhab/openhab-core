/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
package org.openhab.core.io.monitor;

import org.eclipse.jdt.annotation.NonNullByDefault;

import io.micrometer.core.instrument.composite.CompositeMeterRegistry;

/**
 * The {@link MeterRegistryProvider} interface provides a means to retrieve the default OH meter registry instance
 *
 * @author Robert Bach - Initial contribution
 */
@NonNullByDefault
public interface MeterRegistryProvider {

    CompositeMeterRegistry getOHMeterRegistry();
}
