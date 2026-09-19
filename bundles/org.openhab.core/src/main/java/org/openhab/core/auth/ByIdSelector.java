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
package org.openhab.core.auth;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * A {@link Selector} that matches a specific resource by ID. Expression: {@code "id:<value>"}
 *
 * @author Gabor Bicskei - Initial contribution
 *
 * @param id the resource identifier to match
 */
@NonNullByDefault
public record ByIdSelector(String id) implements Selector {

    @Override
    public boolean matches(String resourceId) {
        return id.equals(resourceId);
    }

    @Override
    public String expression() {
        return "id:" + id;
    }
}
