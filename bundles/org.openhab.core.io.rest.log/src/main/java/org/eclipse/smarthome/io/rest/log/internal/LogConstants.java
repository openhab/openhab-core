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
package org.eclipse.smarthome.io.rest.log.internal;

/**
 * The {@link LogConstants} class defines common constants, which are
 * used across the whole module.
 *
 * @author Sebastian Janzen - Initial contribution
 */
public class LogConstants {

    // Log and response message to express, that the log severity addressed is not handled.
    public static final String LOG_SEVERITY_IS_NOT_SUPPORTED = "Your log severity is not supported.";
    public static final String LOG_HANDLE_ERROR = "Internal logging error.";

    // slf4j log pattern to format received log messages, params are URL and message
    public static final String FRONTEND_LOG_PATTERN = "Frontend Log ({}): {}";

    public static final int LOG_BUFFER_LIMIT = 500;

}
