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
package org.openhab.core.thing.firmware;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link FirmwareUpdateResult} enumeration defines the possible results for a firmware update.
 *
 * @author Thomas Höfer - Initial contribution
 */
@NonNullByDefault
public enum FirmwareUpdateResult {

    /** Indicates that the firmware update was successful. */
    SUCCESS,

    /** Indicates that the firmware update has failed. */
    ERROR,

    /** Indicates that the firmware update was canceled. */
    CANCELED;
}
