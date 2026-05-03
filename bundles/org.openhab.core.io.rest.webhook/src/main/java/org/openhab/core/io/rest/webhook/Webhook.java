/*
 * Copyright (c) 2010-2026 Contributors to the openHAB project
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
package org.openhab.core.io.rest.webhook;

import java.net.URL;
import java.time.Instant;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * Result of a successful webhook registration.
 *
 * @param url the publicly reachable URL that proxies requests to the local path
 * @param expiresAt the instant at which this registration expires and will no longer
 *            forward requests unless requested again
 *
 * @author Dan Cunningham - Initial contribution
 */
@NonNullByDefault
public record Webhook(URL url, Instant expiresAt) {
}
