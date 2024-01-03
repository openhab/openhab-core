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
package org.openhab.core.io.rest;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Public constants for the REST API
 *
 * @author Kai Kreuzer - Initial contribution
 */
@NonNullByDefault
public class RESTConstants {

    @Deprecated
    public static final String REST_URI = "/rest";

    public static final String JAX_RS_NAME = "openhab";

    /**
     * Version of the openHAB API
     *
     * Version 1: initial version
     * Version 2: include invisible widgets into sitemap response (#499)
     * Version 3: Addition of anyFormat icon parameter (#978)
     * Version 4: OH3, refactored extensions to addons (#1560)
     * Version 5: transparent charts (#2502)
     * Version 6: extended chart period parameter format (#3863)
     */
    public static final String API_VERSION = "6";
}
