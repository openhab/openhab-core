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
package org.openhab.core.i18n;

import java.time.ZoneId;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * This interface describes a provider for time zone.
 *
 * @author Erdoan Hadzhiyusein - Initial contribution
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
