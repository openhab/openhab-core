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
package org.openhab.core.updater.api;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * This contains common API constants for different Api classes
 *
 * @author Andrew Fiddian-Green - Initial contribution
 */
@NonNullByDefault
public class ApiConstants {

    // Api URIs
    public static final String URI_BASE = "updater";
    public static final String URI_STATUS = "/status";
    public static final String URI_EXECUTE = "/execute";
}
