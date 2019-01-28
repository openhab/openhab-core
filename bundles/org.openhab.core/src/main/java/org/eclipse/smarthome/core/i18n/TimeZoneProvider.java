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
package org.eclipse.smarthome.core.i18n;

import java.time.ZoneId;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * This interface describes a provider for time zone.
 *
 * @author Erdoan Hadzhiyusein - Initial contribution and API
 */
@NonNullByDefault
public interface TimeZoneProvider {

    /**
     * Gets the configured time zone as {@link ZoneId} or the system default time zone if not configured properly.
     *
     * @return the configured time zone as {@link ZoneId} or the system default time zone if not configured properly.
     */
    ZoneId getTimeZone();
}
