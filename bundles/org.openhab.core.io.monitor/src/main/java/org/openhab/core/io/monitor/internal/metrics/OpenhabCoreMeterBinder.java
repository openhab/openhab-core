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
package org.openhab.core.io.monitor.internal.metrics;

import org.eclipse.jdt.annotation.NonNullByDefault;

import io.micrometer.core.instrument.binder.MeterBinder;

/**
 * The {@link OpenhabCoreMeterBinder} interface provides an abstraction of the OH default metrics
 *
 * @author Robert Bach - Initial contribution
 */
@NonNullByDefault
public interface OpenhabCoreMeterBinder extends MeterBinder {
    void unbind();
}
