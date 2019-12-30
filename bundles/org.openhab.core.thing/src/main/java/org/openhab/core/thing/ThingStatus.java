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
package org.openhab.core.thing;

/**
 * {@link ThingStatus} defines possible statuses of a {@link ThingStatusInfo}.
 *
 * @author Stefan Bußweiler - Initial contribution
 * @author Simon Kaufmann - added UNKNOWN, removed numbers
 */
public enum ThingStatus {
    UNINITIALIZED,
    INITIALIZING,
    UNKNOWN,
    ONLINE,
    OFFLINE,
    REMOVING,
    REMOVED;
}
