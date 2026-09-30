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
package org.openhab.core.internal.auth;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * A Gson-serializable DTO for {@link org.openhab.core.auth.Permission}. Uses plain strings
 * for the selector and action fields to avoid Gson interface deserialization issues.
 *
 * @author Gabor Bicskei - Initial contribution
 */
@NonNullByDefault
public class PersistedPermission {

    public String resourceType = "";
    public String selector = "";
    public String action = "";

    public PersistedPermission() {
    }

    public PersistedPermission(String resourceType, String selector, String action) {
        this.resourceType = resourceType;
        this.selector = selector;
        this.action = action;
    }
}
